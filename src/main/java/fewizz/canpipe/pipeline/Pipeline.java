package fewizz.canpipe.pipeline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;

import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.annotation.Nullable;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;


public class Pipeline implements AutoCloseable {

    static record SkyShadows(String framebufferName) {}

    public final ResourceLocation location;
    @Nullable public final SkyShadows skyShadows;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidTerrainFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentEntityFramebuffer;
    public final Framebuffer translucentParticlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    final Map<ResourceLocation, Shader> shaders = new HashMap<>();
    final Map<String, Program> programs = new HashMap<>();
    final Map<String, Texture> textures = new HashMap<>();
    public final Map<String, Framebuffer> framebuffers = new HashMap<>();
    final Map<ResourceLocation, Option> options = new HashMap<>();
    public final Map<ShaderProgram, MaterialProgram> materialPrograms = new HashMap<>();

    private final List<Pass> onInit = new ArrayList<>();
    private final List<Pass> beforeWorldRenderPasses = new ArrayList<>();
    private final List<Pass> fabulousPasses = new ArrayList<>();
    private final List<Pass> afterRenderHandPasses = new ArrayList<>();
    private final List<Pass> onResizePasses = new ArrayList<>();
    private boolean runInitPasses = true;
    private boolean runResizePasses = true;

    public Pipeline(
        ResourceManager manager,
        ResourceLocation location,
        JsonObject pipelineJson
    ) throws
        FileNotFoundException,
        IOException,
        CompilationException,
        NoSuchFieldException,
        SecurityException,
        IllegalArgumentException,
        IllegalAccessException
    {
        // Skipping dynamic options for now... (choosing defaults)
        class SkipDynamicOptions { static JsonElement doSkip(JsonElement e) {
            if (e instanceof JsonObject vo) {
                if (
                    vo.size() == 2 && vo.containsKey("default") &&
                    vo.containsKey("optionMap")
                ) {
                    return (JsonPrimitive) vo.get("default");
                }
                for (var kv : vo.entrySet()) {
                    kv.setValue(doSkip(kv.getValue()));
                }
            }
            if (e instanceof JsonArray va) {
                for (int i = 0; i < va.size(); ++i) {
                    va.set(i, doSkip(va.get(i)));
                }
            }
            return e;
        }}
        SkipDynamicOptions.doSkip(pipelineJson);


        this.location = location;
        int glslVersion = pipelineJson.getInt("glslVersion", 330);
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        JsonObject skyShadowsO = pipelineJson.getObject("skyShadows");

        if (skyShadowsO != null) {
            this.skyShadows = new SkyShadows(skyShadowsO.get(String.class, "framebuffer"));
        }
        else {
            this.skyShadows = null;
        }

        // "options"
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

            this.options.put(includeToken, new Option(includeToken, elements));
        }

        class GLConstantCode {
            static int fromName(String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
                return GL33C.class.getField("GL_"+name).getInt(null);
            }
        }

        // "images"
        for (var textureE : (JsonArray) pipelineJson.get("images")) {
            JsonObject textureO = (JsonObject) textureE;
            String name = textureO.get(String.class, "name");
            int maxLod = textureO.getInt("lod", 0);
            int depth = textureO.getInt("depth", 0);
            int size = textureO.getInt("size", 0);
            int width = textureO.getInt("width", size);
            int height = textureO.getInt("height", size);

            int target = GLConstantCode.fromName(textureO.get(String.class, "target"));
            int internalFormat = GLConstantCode.fromName(textureO.get(String.class, "internalFormat"));
            int pixelFormat = GLConstantCode.fromName(textureO.get(String.class, "pixelFormat"));
            int pixelDataType = GLConstantCode.fromName(textureO.get(String.class, "pixelDataType"));

            List<IntIntPair> params = new ArrayList<>();

            for (var paramsE : textureO.get(JsonArray.class, "texParams")) {
                JsonObject paramsO = (JsonObject) paramsE;
                int name0 = GLConstantCode.fromName(paramsO.get(String.class, "name"));
                int value = GLConstantCode.fromName(paramsO.get(String.class, "val"));
                params.add(IntIntImmutablePair.of(name0, value));
            }

            Texture texture = new Texture(
                location, name,
                new Vector3i(width, height, depth),
                target, internalFormat, pixelFormat,
                pixelDataType, maxLod, params.toArray(new IntIntPair[]{})
            );
            this.textures.put(name, texture);
        }

        // "framebuffers"
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
                    this.textures.get(textureName), clearColor, lod, layer
                ));
            }

            Framebuffer.DepthAttachment depthAttachement = null;
            JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");
            if (depthAttachementO != null) {
                var depthTexture = this.textures.get(depthAttachementO.get(String.class, "image"));

                double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
                depthAttachement = new Framebuffer.DepthAttachment(depthTexture, clearDepth, 0, 0);
            }
            Framebuffer framebuffer = new Framebuffer(location, name, colorAttachements, depthAttachement);
            this.framebuffers.put(name, framebuffer);
        }

        JsonObject targetsO = pipelineJson.getObject("drawTargets");

        // All framebuffers are created, so we can assign target's framebuffers
        this.defaultFramebuffer = this.framebuffers.get(pipelineJson.get(String.class, "defaultFramebuffer"));
        this.solidTerrainFramebuffer = this.framebuffers.get(targetsO.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentTerrain"));
        this.translucentEntityFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentEntity"));
        this.translucentParticlesFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = this.framebuffers.get(targetsO.get(String.class, "weather"));
        this.cloudsFramebuffer = this.framebuffers.get(targetsO.get(String.class, "clouds"));

        // "programs"
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

            class Shaders { Shader getOrLoad(ResourceLocation location, Type type) throws CompilationException, IOException {
                Shader shader = shaders.get(location);
                if (shader == null) {
                    shader = Shader.compile(location, type, glslVersion, options, IOUtils.toString(manager.openAsReader(location)));
                    shaders.put(location, shader);
                }
                return shader;
            }}

            Shader vertex = new Shaders().getOrLoad(vertexLoc, Type.VERTEX);
            Shader fragment = new Shaders().getOrLoad(fragmentLoc, Type.FRAGMENT);

            Program program = new Program(location.withSuffix("-"+name), samplers, vertex, fragment);
            this.programs.put(name, program);
        }

        // passes
        BiConsumer<String, List<Pass>> parsePasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson == null) {
                return;
            }
            for (var passE : passesJson.get(JsonArray.class, "passes")) {
                JsonObject passO = (JsonObject) passE;

                String passName = passO.get(String.class, "name");
                Framebuffer framebuffer = this.framebuffers.get(passO.get(String.class, "framebuffer"));
                String programName = passO.get(String.class, "program");

                Pass pass;

                if (programName.equals("frex_clear")) {
                    pass = new Pass.FREXClear(passName, framebuffer);
                }
                else {
                    Program program = this.programs.get(programName);

                    JsonArray samplersA = passO.get(JsonArray.class, "samplerImages");
                    List<AbstractTexture> samplers = new ArrayList<>();
                    for (String s : samplersA.stream().map(e -> ((JsonPrimitive)e).asString()).toList()) {
                        if (s.contains(":")) {
                            samplers.add(new SimpleTexture(ResourceLocation.parse(s)));
                        }
                        else {
                            samplers.add(this.textures.get(s));
                        }
                    }

                    int size = passO.getInt("size", 0);
                    int width = passO.getInt("width", size);
                    int height = passO.getInt("height", size);
                    int lod = passO.getInt("lod", 0);
                    int layer = passO.getInt("layer", 0);

                    pass = new Pass(passName, framebuffer, program, samplers, new Vector2i(width, height), lod, layer);
                }

                passes.add(pass);
            }
        };

        parsePasses.accept("onInit", this.onInit);
        parsePasses.accept("beforeWorldRender", this.beforeWorldRenderPasses);
        parsePasses.accept("fabulous", this.fabulousPasses);
        parsePasses.accept("afterRenderHand", this.afterRenderHandPasses);
        parsePasses.accept("afterRenderHand", this.onResizePasses);

        if (skyShadows != null) {
            this.beforeWorldRenderPasses.addFirst(new Pass.FREXClear(
                "can_pipe_clear_shadow_map",
                this.framebuffers.get(skyShadows.framebufferName))
            );
        }

        // "materialProgram"
        JsonObject materailProgramO = pipelineJson.getObject("materialProgram");

        var materialVertex = ResourceLocation.parse(materailProgramO.get(String.class, "vertexSource"));
        var materialFragment = ResourceLocation.parse(materailProgramO.get(String.class, "fragmentSource"));

        List<ShaderProgramConfig.Sampler> materialSamplers = new ArrayList<>();

        var materialSamplersA = materailProgramO.get(JsonArray.class, "samplers");
        if (materialSamplersA != null) {
            materialSamplers = materialSamplersA.stream().map(
                s -> new ShaderProgramConfig.Sampler(((JsonPrimitive)s).asString())
            ).toList();
        }

        List<AbstractTexture> materialSamplerImages = new ArrayList<>();
        var materialSamplerImagesA = materailProgramO.get(JsonArray.class, "samplerImages");
        if (materialSamplerImagesA != null) {
            for (String s : materialSamplerImagesA.stream().map(e -> ((JsonPrimitive)e).asString()).toList()) {
                if (s.contains(":")) {
                    materialSamplerImages.add(new SimpleTexture(ResourceLocation.parse(s)));
                }
                else {
                    materialSamplerImages.add(this.textures.get(s));
                }
            }
        }

        for (var sp : new ShaderProgram[] {
            CoreShaders.RENDERTYPE_SOLID,
            CoreShaders.RENDERTYPE_CUTOUT_MIPPED,
            CoreShaders.RENDERTYPE_CUTOUT,
            CoreShaders.RENDERTYPE_TRANSLUCENT
        }) {
            var program = MaterialProgram.create(
                sp,
                glslVersion,
                enablePBR,
                skyShadows != null ? this.framebuffers.get(skyShadows.framebufferName) : null,
                location,
                materialVertex,
                materialFragment,
                this.options,
                materialSamplers,
                materialSamplerImages
            );
            this.materialPrograms.put(sp, program);
        }
    }

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> {
            t.onWindowSizeChanged(w, h);
        });
        this.framebuffers.forEach((n, f) -> {
            f.resize(w, h);
        });
        this.runResizePasses = true;
    }

    public void onBeforeWorldRender(Matrix4f view, Matrix4f projection) {
        if (this.runInitPasses) {
            for (Pass pass : this.onInit) {
                pass.apply(view, projection);
            }
            this.runInitPasses = false;
        }

        if (this.runResizePasses) {
            for (Pass pass : this.onResizePasses) {
                pass.apply(view, projection);
            }
            this.runResizePasses = false;
        }

        for (Pass pass : this.beforeWorldRenderPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
    }

    public void onAfterWorldRender(Matrix4f view, Matrix4f projection) {
        for (Pass pass : this.fabulousPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
    }

    public void onAfterRenderHand(Matrix4f view, Matrix4f projection) {
        for (Pass pass : this.afterRenderHandPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
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
