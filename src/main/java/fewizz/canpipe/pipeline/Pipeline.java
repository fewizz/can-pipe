package fewizz.canpipe.pipeline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL40C;

import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.annotation.Nullable;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record SkyShadows(
        String framebufferName,
        ResourceLocation vertexShaderLocation,
        ResourceLocation fragmentShaderLocation,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits
    ) {}

    public static record Sky(
        float defaultZenithAngle
    ) {}

    public final ResourceLocation location;
    @Nullable public final SkyShadows skyShadows;
    @Nullable public final Sky sky;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentItemEntityFramebuffer;
    public final Framebuffer particlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    final Map<Pair<ResourceLocation, Type>, Shader> shaders = new HashMap<>();
    final Map<String, Program> programs = new HashMap<>();
    final Map<String, Texture> textures = new HashMap<>();
    public final Map<String, Framebuffer> framebuffers = new HashMap<>();
    final Map<ResourceLocation, Option> options = new HashMap<>();
    public final Map<VertexFormat, MaterialProgram> materialPrograms = new HashMap<>();
    public final Map<VertexFormat, MaterialProgram> shadowPrograms = new HashMap<>();

    private final List<PassBase> onInit = new ArrayList<>();
    private final List<PassBase> beforeWorldRenderPasses = new ArrayList<>();
    private final List<PassBase> fabulousPasses = new ArrayList<>();
    private final List<PassBase> afterRenderHandPasses = new ArrayList<>();
    private final List<PassBase> onResizePasses = new ArrayList<>();
    private boolean runInitPasses = true;
    private boolean runResizePasses = true;

    public Pipeline(
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
        this.location = location;
        var mc = Minecraft.getInstance();

        // "options"
        for (var optionO : JanksonUtils.listOfObjects(pipelineJson, "options")) {
            ResourceLocation includeToken = ResourceLocation.parse(optionO.get(String.class, "includeToken"));
            var elementsO = optionO.getObject("elements");
            // compat
            if (elementsO == null) {
                elementsO = optionO.getObject("options");
            }
            if (elementsO != null) {
                Map<String, Option.Element> elements = new HashMap<>();
                for (var elementE : elementsO.entrySet()) {
                    String name = elementE.getKey();
                    JsonObject elementO = (JsonObject) elementE.getValue();
                    var defaultValue = elementO.get(JsonPrimitive.class, "default");
                    var prefix = elementO.get(String.class, "prefix");
                    elements.put(name, new Option.Element(defaultValue, prefix));
                }
                this.options.put(includeToken, new Option(includeToken, elements));
            }
        }
        pipelineJson.remove("options");

        Function<String, Option.Element> optionElementByName = (String name) -> {
            for (var o : options.values()) {
                if (o.elements.containsKey(name)) {
                    return o.elements.get(name);
                }
            }
            return null;
        };

        // Skipping dynamic options for now... (choosing defaults)
        class SkipDynamicOptions { static JsonElement doSkip(JsonElement e) {
            if (e instanceof JsonObject vo) {
                if (vo.size() == 2 && vo.containsKey("default") && (vo.containsKey("optionMap") || vo.containsKey("option"))) {
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

        class ApplyOptions { static JsonElement doApply(JsonElement e, Function<String, Option.Element> optionElementByName) {
            if (e instanceof JsonObject vo) {
                if (vo.size() == 1 && vo.containsKey("option")) {
                    String optionName = vo.get(String.class, "option");
                    return optionElementByName.apply(optionName).defaultValue;
                }
                for (var kv : vo.entrySet()) {
                    kv.setValue(doApply(kv.getValue(), optionElementByName));
                }
            }
            if (e instanceof JsonArray va) {
                for (int i = 0; i < va.size(); ++i) {
                    va.set(i, doApply(va.get(i), optionElementByName));
                }
            }
            return e;
        }}
        ApplyOptions.doApply(pipelineJson, optionElementByName);

        int glslVersion = pipelineJson.getInt("glslVersion", 330);
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        JsonObject skyShadowsO = pipelineJson.getObject("skyShadows");

        if (skyShadowsO != null) {
            this.skyShadows = new SkyShadows(
                skyShadowsO.get(String.class, "framebuffer"),
                ResourceLocation.parse(skyShadowsO.get(String.class, "vertexSource")),
                ResourceLocation.parse(skyShadowsO.get(String.class, "fragmentSource")),
                JanksonUtils.listOfIntegers(skyShadowsO, "cascadeRadius"),
                skyShadowsO.getFloat("offsetSlopeFactor", 1.1F),
                skyShadowsO.getFloat("offsetBiasUnits", 4.0F)
            );
        }
        else {
            this.skyShadows = null;
        }

        JsonObject skyO = pipelineJson.getObject("sky");
        if (skyO != null) {
            this.sky = new Sky(
                (float) Math.toRadians(skyO.getFloat("defaultZenithAngle", 0.0F))
            );
        }
        else {
            this.sky = null;
        }

        // "images"
        class GLConstantCode {
            static int fromName(String name) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
                // Not 3.3, because GL_TEXTURE_CUBE_MAP_ARRAY is in 4.0
                return GL40C.class.getField("GL_"+name).getInt(null);
            }
        }

        for (var textureO : JanksonUtils.listOfObjects(pipelineJson, "images")) {
            String name = textureO.get(String.class, "name");
            int maxLod = textureO.getInt("lod", 0);
            int depth = textureO.getInt("depth", 0);
            int size = textureO.getInt("size", 0);
            int width = textureO.getInt("width", size);
            int height = textureO.getInt("height", size);

            String targetStr = textureO.get(String.class, "target");
            int target = targetStr != null ? GLConstantCode.fromName(targetStr) : GL33C.GL_TEXTURE_2D;

            String internalFormatStr = textureO.get(String.class, "internalFormat");
            int internalFormat = internalFormatStr != null ? GLConstantCode.fromName(internalFormatStr) : GL33C.GL_RGBA8;

            String pixelFormatStr = textureO.get(String.class, "pixelFormat");
            int pixelFormat = pixelFormatStr != null ? GLConstantCode.fromName(pixelFormatStr) : GL33C.GL_RGBA;

            String pixelDataTypeStr = textureO.get(String.class, "pixelDataType");
            int pixelDataType = pixelDataTypeStr != null ? GLConstantCode.fromName(pixelDataTypeStr) : GL33C.GL_UNSIGNED_BYTE;

            List<IntIntPair> params = new ArrayList<>();

            for (var paramsO : JanksonUtils.listOfObjects(textureO, "texParams")) {
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
        for (var framebufferO : JanksonUtils.listOfObjects(pipelineJson, "framebuffers")) {
            String name = framebufferO.get(String.class, "name");
            List<Framebuffer.ColorAttachment> colorAttachements = new ArrayList<>();

            for (var colorAttachementO : JanksonUtils.listOfObjects(framebufferO, "colorAttachments")) {
                String textureName = colorAttachementO.get(String.class, "image");
                int lod = colorAttachementO.getInt("lod", 0);
                int layer = colorAttachementO.getInt("layer", 0);
                int face = colorAttachementO.getInt("face", -1);

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
                    this.textures.get(textureName), clearColor, lod, layer, face
                ));
            }

            Framebuffer.DepthAttachment depthAttachement = null;
            JsonObject depthAttachementO = framebufferO.getObject("depthAttachment");
            if (depthAttachementO != null) {
                var depthTexture = this.textures.get(depthAttachementO.get(String.class, "image"));

                double clearDepth = depthAttachementO.getDouble("clearDepth", 1.0);
                depthAttachement = new Framebuffer.DepthAttachment(
                    depthTexture,
                    clearDepth,
                    Optional.ofNullable(depthAttachementO.get(Integer.class, "lod")),
                    Optional.ofNullable(depthAttachementO.get(Integer.class, "layer"))
                );
            }
            Framebuffer framebuffer = new Framebuffer(location, name, colorAttachements, depthAttachement);
            this.framebuffers.put(name, framebuffer);
        }

        Framebuffer shadowFramebuffer = skyShadows != null ? this.framebuffers.get(skyShadows.framebufferName) : null;

        JsonObject targetsO = pipelineJson.getObject("drawTargets");

        this.defaultFramebuffer = this.framebuffers.get(pipelineJson.get(String.class, "defaultFramebuffer"));
        this.solidFramebuffer = this.framebuffers.get(targetsO.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentTerrain"));
        this.translucentItemEntityFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentEntity"));
        this.particlesFramebuffer = this.framebuffers.get(targetsO.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = this.framebuffers.get(targetsO.get(String.class, "weather"));
        this.cloudsFramebuffer = this.framebuffers.get(targetsO.get(String.class, "clouds"));

        Map<ResourceLocation, String> shaderSourceCache = new HashMap<>();

        // "programs"
        for (var programO : JanksonUtils.listOfObjects(pipelineJson, "programs")) {
            String name = programO.get(String.class, "name");

            List<String> samplers = JanksonUtils.listOfStrings(programO, "samplers");

            var vertexLoc = ResourceLocation.parse(programO.get(String.class, "vertexSource"));
            var fragmentLoc = ResourceLocation.parse(programO.get(String.class, "fragmentSource"));

            class Shaders { Shader getOrLoad(ResourceLocation location, Type type) throws CompilationException, IOException {
                var p = Pair.of(location, type);
                Shader shader = shaders.get(p);
                if (shader == null) {
                    String source = shaderSourceCache.get(location);
                    if (source == null) {
                        source = IOUtils.toString(mc.getResourceManager().openAsReader(location));
                        shaderSourceCache.put(location, source);
                    }
                    shader = Shader.compile(location, type, glslVersion, options, source, shaderSourceCache, shadowFramebuffer);
                    shaders.put(p, shader);
                }
                return shader;
            }}

            Shader vertex = new Shaders().getOrLoad(vertexLoc, Type.VERTEX);
            Shader fragment = new Shaders().getOrLoad(fragmentLoc, Type.FRAGMENT);

            Program program = new Program(location.withSuffix("-"+name), samplers, vertex, fragment);
            this.programs.put(name, program);
        }

        // passes
        BiConsumer<String, List<PassBase>> parsePasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson == null) {
                return;
            }
            for (var passO : JanksonUtils.listOfObjects(passesJson, "passes")) {
                String toggleConfig = passO.get(String.class, "toggleConfig");

                // pass is disabled, skipping
                if (toggleConfig != null && !optionElementByName.apply(toggleConfig).defaultValue.asBoolean(true)) {
                    continue;
                }

                String passName = passO.get(String.class, "name");
                Framebuffer framebuffer = this.framebuffers.get(passO.get(String.class, "framebuffer"));
                String programName = passO.get(String.class, "program");

                PassBase pass;

                if (programName.equals("frex_clear")) {
                    pass = new Pass.FREXClear(passName, framebuffer);
                }
                else {
                    Program program = this.programs.get(programName);
                    Objects.nonNull(program);

                    List<AbstractTexture> textures = new ArrayList<>();
                    for (String s : JanksonUtils.listOfStrings(passO, "samplerImages")) {
                        AbstractTexture t = null;
                        if (s.contains(":")) {
                            t = mc.getTextureManager().getTexture(ResourceLocation.parse(s));
                        }
                        else {
                            t = this.textures.get(s);
                        }
                        textures.add(t);
                    }

                    int size = passO.getInt("size", 0);
                    int width = passO.getInt("width", size);
                    int height = passO.getInt("height", size);
                    int lod = passO.getInt("lod", 0);
                    int layer = passO.getInt("layer", 0);

                    pass = new Pass(passName, framebuffer, program, textures, new Vector2i(width, height), lod, layer);
                }

                passes.add(pass);
            }
        };

        parsePasses.accept("onInit", this.onInit);
        parsePasses.accept("onResize", this.onResizePasses);

        parsePasses.accept("beforeWorldRender", this.beforeWorldRenderPasses);
        parsePasses.accept("fabulous", this.fabulousPasses);
        parsePasses.accept("afterRenderHand", this.afterRenderHandPasses);

        if (skyShadows != null) {
            this.beforeWorldRenderPasses.addFirst(new Pass.FREXClear(
                "can_pipe_clear_shadow_map",
                this.framebuffers.get(skyShadows.framebufferName))
            );
        }

        // "materialProgram"
        JsonObject materailProgramO = pipelineJson.getObject("materialProgram");

        var materialVertexShaderLocation = ResourceLocation.parse(materailProgramO.get(String.class, "vertexSource"));
        var materialFragmentShaderLocation = ResourceLocation.parse(materailProgramO.get(String.class, "fragmentSource"));

        List<String> samplers = JanksonUtils.listOfStrings(materailProgramO, "samplers");
        List<? extends AbstractTexture> samplerImages = new ArrayList<>() {{
            for (String textureName : JanksonUtils.listOfStrings(materailProgramO, "samplerImages")) {
                if (textureName.contains(":")) {
                    add(mc.getTextureManager().getTexture(ResourceLocation.parse(textureName)));
                }
                else {
                    add(textures.get(textureName));
                }
            }
        }};

        for (var vertexFormat : new VertexFormat[] {
            CanPipe.VertexFormats.BLOCK,
            CanPipe.VertexFormats.NEW_ENTITY,
            CanPipe.VertexFormats.PARTICLE,
        }) {
            this.materialPrograms.put(
                vertexFormat, MaterialProgram.create(
                    vertexFormat,
                    glslVersion,
                    enablePBR,
                    false,
                    shadowFramebuffer,
                    location,
                    materialVertexShaderLocation,
                    materialFragmentShaderLocation,
                    this.options,
                    samplers,
                    samplerImages,
                    shaderSourceCache
                )
            );

            if (this.skyShadows != null) {
                this.shadowPrograms.put(
                    vertexFormat, MaterialProgram.create(
                        vertexFormat,
                        glslVersion,
                        enablePBR,
                        true, // depth pass
                        shadowFramebuffer,
                        location,
                        this.skyShadows.vertexShaderLocation,
                        this.skyShadows.fragmentShaderLocation,
                        this.options,
                        List.of(),
                        List.of(),
                        shaderSourceCache
                    )
                );
            }
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
        for (MaterialProgram p : this.materialPrograms.values()) {
            if (p.CANPIPE_ORIGIN_TYPE != null) {
                p.CANPIPE_ORIGIN_TYPE.set(0);  // camera
            }
        }

        if (this.runInitPasses) {
            for (PassBase pass : this.onInit) {
                pass.apply(view, projection);
            }
            this.runInitPasses = false;
        }

        if (this.runResizePasses) {
            for (PassBase pass : this.onResizePasses) {
                pass.apply(view, projection);
            }
            this.runResizePasses = false;
        }

        for (PassBase pass : this.beforeWorldRenderPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);
    }

    public void onAfterWorldRender(Matrix4f view, Matrix4f projection) {
        for (PassBase pass : this.fabulousPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);

        for (MaterialProgram p : this.materialPrograms.values()) {
            if (p.CANPIPE_ORIGIN_TYPE != null) {
                p.CANPIPE_ORIGIN_TYPE.set(3);  // hand
            }
        }
    }

    public void onAfterRenderHand(Matrix4f view, Matrix4f projection) {
        for (PassBase pass : this.afterRenderHandPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);

        for (MaterialProgram p : this.materialPrograms.values()) {
            if (p.CANPIPE_ORIGIN_TYPE != null) {
                p.CANPIPE_ORIGIN_TYPE.set(2);  // screen
            }
        }
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = level.getSunAngle(0.0F);
        float zenithAngle = this.sky != null ? this.sky.defaultZenithAngle() : 0.0F;
        long ticks = (level.dimensionType().fixedTime().orElse(level.getDayTime())) % 24000L;

        result.set(
            (float) (-Math.sin(hourAngle)),
            (float) ( Math.cos(hourAngle) *  Math.cos(zenithAngle)),
            (float) ( Math.cos(hourAngle) * -Math.sin(zenithAngle))
        );

        if (ticks > 13000L && ticks < 23000L) {  // moon
            result.negate();
        }

        return result;
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
