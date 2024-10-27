package fewizz.canpipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.shaders.CompiledShader.Type;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;


public class Pipeline implements AutoCloseable {
    public final ResourceLocation location;
    public final ResourceLocation materialVertexShaderLocation;
    public final ResourceLocation materialFragmentShaderLocation;
    public final int glslVersion;
    public final boolean enablePBR;
    public final String defaultFramebufferName;
    public final String solidTerrainFramebufferName;
    public final String translucentTerrainFramebufferName;
    public final String translucentEntityFramebufferName;
    public final String weatherFramebufferName;
    public final String cloudsFramebufferName;
    public final String translucentParticlesFramebufferName;

    final Map<String, Shader> shaders = new HashMap<>();
    final Map<String, Program> programs = new HashMap<>();
    final Map<String, Texture> textures = new HashMap<>();
    public final Map<String, Framebuffer> framebuffers = new HashMap<>();
    final Map<ResourceLocation, Option> options = new HashMap<>();
    final List<Pass> beforeWorldRender = new ArrayList<>();
    final List<Pass> fabulous = new ArrayList<>();
    final List<Pass> afterRenderHand = new ArrayList<>();
    public final Map<ShaderProgram, MaterialProgram> materialPrograms = new HashMap<>();

    public Framebuffer getDefaultFramebuffer() {
        return this.framebuffers.get(this.defaultFramebufferName);
    }

    private Pipeline(
        ResourceLocation location,
        ResourceLocation materialVertexShaderLocation,
        ResourceLocation materialFragmentShaderLocation,
        int glslVersion,
        boolean enablePBR,
        String defaultFramebufferName,
        String solidTerrainFramebufferName,
        String translucentTerrainFramebufferName,
        String translucentEntityFramebufferName,
        String weatherFramebufferName,
        String cloudsFramebufferName,
        String translucentParticlesFramebufferName
    ) {
        this.location = location;
        this.materialVertexShaderLocation = materialVertexShaderLocation;
        this.materialFragmentShaderLocation = materialFragmentShaderLocation;
        this.glslVersion = glslVersion;
        this.enablePBR = enablePBR;

        this.defaultFramebufferName = defaultFramebufferName;
        this.solidTerrainFramebufferName = solidTerrainFramebufferName;
        this.translucentTerrainFramebufferName = translucentTerrainFramebufferName;
        this.translucentEntityFramebufferName = translucentEntityFramebufferName;
        this.weatherFramebufferName = weatherFramebufferName;
        this.cloudsFramebufferName = cloudsFramebufferName;
        this.translucentParticlesFramebufferName = translucentParticlesFramebufferName;
    }

    static Pipeline createFromData(
        ResourceManager manager,
        ResourceLocation location,
        JsonObject pipelineJson
    ) throws IOException, SyntaxError, CompilationException {
        int glslVersion = pipelineJson.getInt("glslVersion", 330);
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        boolean hasSkyShadows = pipelineJson.containsKey("skyShadows");

        JsonObject materailProgramO = pipelineJson.getObject("materialProgram");

        var materialVertex = ResourceLocation.parse(materailProgramO.get(String.class, "vertexSource"));
        var materialFragment = ResourceLocation.parse(materailProgramO.get(String.class, "fragmentSource"));

        String defaultFramebufferName = pipelineJson.get(String.class, "defaultFramebuffer");

        JsonObject targetsO = pipelineJson.getObject("drawTargets");

        @SuppressWarnings("resource")  // TODO
        Pipeline p = new Pipeline(
            location, materialVertex, materialFragment,
            glslVersion, enablePBR, defaultFramebufferName,
            targetsO.get(String.class, "solidTerrain"),
            targetsO.get(String.class, "translucentTerrain"),
            targetsO.get(String.class, "translucentEntity"),
            targetsO.get(String.class, "weather"),
            targetsO.get(String.class, "clouds"),
            targetsO.get(String.class, "translucentParticles")
        );

        class SkipDynamicOptions { static void doSkip(JsonObject o) {
            for (var kv : o.entrySet()) {
                var k = kv.getKey();
                var v = kv.getValue();

                if (v instanceof JsonObject vo) {
                    if (vo.size() == 2 && vo.containsKey("default") && vo.containsKey("optionMap")) {
                        o.put(k, vo.get("default"));
                    }
                    else {
                        doSkip(vo);
                    }
                }
                if (v instanceof JsonArray va) {
                    for (var e : va) {
                        if (e instanceof JsonObject) {
                            doSkip((JsonObject) e);
                        }
                    }
                }
            }
        }}
        SkipDynamicOptions.doSkip(pipelineJson);

        for (var optionsE : (JsonArray) pipelineJson.get("options")) {
            JsonObject optionO = (JsonObject) optionsE;
            ResourceLocation includeToken = ResourceLocation.parse(optionO.get(String.class, "includeToken"));

            Map<String, Option.Element> elements = new HashMap<>();
            for (var elementE : optionO.getObject("elements").entrySet()) {
                String name = elementE.getKey();
                JsonObject elementO = (JsonObject) elementE.getValue();
                var defaultValue = elementO.get(JsonPrimitive.class, "default");
                elements.put(name, new Option.Element(defaultValue));
            }

            p.options.put(includeToken, new Option(includeToken, elements));
        }

        for (var ps : List.of(
            CoreShaders.RENDERTYPE_SOLID,
            CoreShaders.RENDERTYPE_CUTOUT_MIPPED,
            CoreShaders.RENDERTYPE_CUTOUT,
            CoreShaders.RENDERTYPE_TRANSLUCENT
        )) {
            var program = MaterialProgram.create(
                ps, glslVersion, enablePBR, hasSkyShadows, location, materialVertex, materialFragment, p.options
            );
            p.materialPrograms.put(ps, program);
        }

        for (var textureE : (JsonArray) pipelineJson.get("images")) {
            JsonObject textureO = (JsonObject) textureE;
            String name = textureO.get(String.class, "name");
            int maxLod = textureO.getInt("lod", 0);
            int depth = textureO.getInt("depth", 0);
            int size = textureO.getInt("size", 0);
            int width = textureO.getInt("width", size);
            int height = textureO.getInt("height", size);

            int target = new HashMap<String, Integer>() {{
                put("TEXTURE_2D", GL33C.GL_TEXTURE_2D);
                put("TEXTURE_2D_ARRAY", GL33C.GL_TEXTURE_2D_ARRAY);
                put("TEXTURE_CUBE_MAP", GL33C.GL_TEXTURE_CUBE_MAP);
            }}.computeIfAbsent(textureO.get(String.class, "target"), n -> {throw new NotImplementedException(n);});
            int internalFormat = new HashMap<String, Integer>() {{
                put("R32F", GL33C.GL_R32F);
                put("RG16", GL33C.GL_RG16);
                put("RGB", GL33C.GL_RGB);
                put("RGB8", GL33C.GL_RGB8);
                put("RGB16", GL33C.GL_RGB16);
                put("RGB16F", GL33C.GL_RGB16F);
                put("RGB32UI", GL33C.GL_RGB32UI);
                put("R11F_G11F_B10F", GL33C.GL_R11F_G11F_B10F);
                put("RGBA8", GL33C.GL_RGBA8);
                put("RGBA16F", GL33C.GL_RGBA16F);
                put("DEPTH_COMPONENT", GL33C.GL_DEPTH_COMPONENT);
                put("DEPTH_COMPONENT32", GL33C.GL_DEPTH_COMPONENT32);
            }}.computeIfAbsent(textureO.get(String.class, "internalFormat"), n -> {throw new NotImplementedException(n);});
            int pixelFormat = new HashMap<String, Integer>() {{
                put("RED", GL33C.GL_RED);
                put("RG", GL33C.GL_RG);
                put("RGB", GL33C.GL_RGB);
                put("RGBA", GL33C.GL_RGBA);
                put("RGB_INTEGER", GL33C.GL_RGB_INTEGER);
                put("DEPTH_COMPONENT", GL33C.GL_DEPTH_COMPONENT);
            }}.computeIfAbsent(textureO.get(String.class, "pixelFormat"), n -> {throw new NotImplementedException(n);});
            int pixelDataType = new HashMap<String, Integer>() {{
                put("UNSIGNED_BYTE", GL33C.GL_UNSIGNED_BYTE);
                put("UNSIGNED_INT", GL33C.GL_UNSIGNED_INT);
                put("FLOAT", GL33C.GL_FLOAT);
            }}.computeIfAbsent(textureO.get(String.class, "pixelDataType"), n -> {throw new NotImplementedException(n);});

            List<IntIntPair> params = new ArrayList<>();

            for (var paramsE : textureO.get(JsonArray.class, "texParams")) {
                JsonObject paramsO = (JsonObject) paramsE;
                int name0 = new HashMap<String, Integer>() {{
                    put("TEXTURE_MIN_FILTER", GL33C.GL_TEXTURE_MIN_FILTER);
                    put("TEXTURE_MAG_FILTER", GL33C.GL_TEXTURE_MAG_FILTER);
                    put("TEXTURE_WRAP_S", GL33C.GL_TEXTURE_WRAP_S);
                    put("TEXTURE_WRAP_T", GL33C.GL_TEXTURE_WRAP_T);
                    put("TEXTURE_WRAP_R", GL33C.GL_TEXTURE_WRAP_R);
                    put("TEXTURE_COMPARE_MODE", GL33C.GL_TEXTURE_COMPARE_MODE);
                    put("TEXTURE_COMPARE_FUNC", GL33C.GL_TEXTURE_COMPARE_FUNC);
                }}.computeIfAbsent(paramsO.get(String.class, "name"), n -> {throw new NotImplementedException(n);});
                int value = new HashMap<String, Integer>() {{
                    put("NONE", GL33C.GL_NONE);
                    put("NEAREST", GL33C.GL_NEAREST);
                    put("LINEAR", GL33C.GL_LINEAR);
                    put("NEAREST_MIPMAP_NEAREST", GL33C.GL_NEAREST_MIPMAP_NEAREST);
                    put("LINEAR_MIPMAP_NEAREST", GL33C.GL_LINEAR_MIPMAP_NEAREST);
                    put("LINEAR_MIPMAP_LINEAR", GL33C.GL_LINEAR_MIPMAP_LINEAR);
                    put("CLAMP_TO_EDGE", GL33C.GL_CLAMP_TO_EDGE);
                    put("COMPARE_REF_TO_TEXTURE", GL33C.GL_COMPARE_REF_TO_TEXTURE);
                    put("LEQUAL", GL33C.GL_LEQUAL);
                }}.computeIfAbsent(paramsO.get(String.class, "val"), n -> {throw new NotImplementedException(n);});
                params.add(IntIntImmutablePair.of(name0, value));
            }

            Texture texture = new Texture(
                name, new Vector3i(width, height, depth), target, internalFormat, pixelFormat,
                pixelDataType, maxLod, params.toArray(new IntIntPair[]{})
            );
            p.textures.put(name, texture);
        }

        for (var framebufferE : pipelineJson.get(JsonArray.class, "framebuffers")) {
            var framebufferO = (JsonObject) framebufferE;
            String name = framebufferO.get(String.class, "name");
            List<Framebuffer.ColorAttachment> colorAttachements = new ArrayList<>();

            for (var colorAttachmentE : Objects.requireNonNullElse(framebufferO.get(JsonArray.class, "colorAttachments"), new JsonArray())) {
                var colorAttachementO = (JsonObject) colorAttachmentE;
                String textureName = colorAttachementO.get(String.class, "image");
                int lod = colorAttachementO.getInt("lod", 0);
                int layer = colorAttachementO.getInt("layer", 0);

                Vector4f clearColor = new Vector4f(0.0F);
                JsonElement clearColorRaw = colorAttachementO.get("clearColor");
                if (clearColorRaw != null) {
                    Object clearColorO = ((JsonPrimitive) clearColorRaw).getValue();
                    if (clearColorO instanceof Long l) {
                        clearColor.x = ((l >> 24) & 0xFF) / 255f;
                        clearColor.y = ((l >> 16) & 0xFF) / 255f;
                        clearColor.z = ((l >> 8)  & 0xFF) / 255f;
                        clearColor.w = ((l >> 0)  & 0xFF) / 255f;
                    }
                    else {
                        throw new NotImplementedException(clearColorO.getClass().getName());
                    }
                }

                colorAttachements.add(new Framebuffer.ColorAttachment(
                    p.textures.get(textureName), clearColor, lod, layer
                ));
            }

            Framebuffer.DepthAttachment depthAttachement = null;
            JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");
            if (depthAttachementO != null) {
                var depthTexture = p.textures.get(depthAttachementO.get(String.class, "image"));

                double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
                depthAttachement = new Framebuffer.DepthAttachment(depthTexture, clearDepth, 0, 0);
            }
            Framebuffer framebuffer = new Framebuffer(colorAttachements, depthAttachement);
            p.framebuffers.put(name, framebuffer);
        }

        for (var programE : (JsonArray) Objects.requireNonNullElse(pipelineJson.get("programs"), new JsonArray())) {
            JsonObject programO = (JsonObject) programE;
            String name = programO.get(String.class, "name");

            List<ShaderProgramConfig.Sampler> samplers = List.of();
            var samplersA = programO.get(JsonArray.class, "samplers");
            if (samplersA != null) {
                samplers = samplersA.stream().map(
                    s -> new ShaderProgramConfig.Sampler(((JsonPrimitive)s).asString())
                ).toList();
            }

            var vertexLoc = ResourceLocation.parse(programO.get(String.class, "vertexSource"));
            var fragmentLoc = ResourceLocation.parse(programO.get(String.class, "fragmentSource"));

            Shader vertex = Shader.compile(vertexLoc, Type.VERTEX, glslVersion, p.options, IOUtils.toString(manager.openAsReader(vertexLoc)));
            Shader fragment = Shader.compile(fragmentLoc, Type.FRAGMENT, glslVersion, p.options, IOUtils.toString(manager.openAsReader(fragmentLoc)));

            Program program = new Program(location.withSuffix("-"+name), samplers, vertex, fragment);
            p.programs.put(name, program);
        }

        BiConsumer<String, List<Pass>> parsePasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson == null) {
                return;
            }
            for (var passE : passesJson.get(JsonArray.class, "passes")) {
                JsonObject passO = (JsonObject) passE;

                String passName = passO.get(String.class, "name");
                Framebuffer framebuffer = p.framebuffers.get(passO.get(String.class, "framebuffer"));
                String programName = passO.get(String.class, "program");

                Pass pass;

                if (programName.equals("frex_clear")) {
                    pass = new Pass.FrexClear(passName, framebuffer);
                }
                else {
                    Program program = p.programs.get(programName);

                    JsonArray samplersA = passO.get(JsonArray.class, "samplerImages");
                    List<AbstractTexture> samplers = new ArrayList<>();
                    for (String s : samplersA.stream().map(e -> ((JsonPrimitive)e).asString()).toList()) {
                        if (s.contains(":")) {
                            throw new NotImplementedException();
                        }
                        samplers.add(p.textures.get(s));
                    }
        
                    int lod = passO.getInt("lod", 0);
                    int layer = passO.getInt("layer", 0);
                    
                    pass = new Pass(passName, framebuffer, program, samplers, lod, layer);
                }

                passes.add(pass);
            }
        };

        parsePasses.accept("beforeWorldRender", p.beforeWorldRender);
        parsePasses.accept("fabulous", p.fabulous);
        parsePasses.accept("afterRenderHand", p.afterRenderHand);

        return p;
    }

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> {
            t.onWindowSizeChanged(w, h);
        });
        this.framebuffers.forEach((n, f) -> {
            f.resize(w, h);
        });
    }

    @Override
    public void close() {
        this.framebuffers.forEach((n, f) -> {
            f.destroyBuffers();
        });
        this.textures.forEach((n, t) -> {
            t.close();
        });
        this.shaders.forEach((n, s) -> {
            s.close();
        });
        this.programs.forEach((n, p) -> {
            p.close();
        });
    }

}
