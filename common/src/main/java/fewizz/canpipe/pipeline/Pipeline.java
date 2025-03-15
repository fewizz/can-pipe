package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record SkyShadows(
        Framebuffer framebuffer,
        ResourceLocation vertexShaderLocation,
        ResourceLocation fragmentShaderLocation,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits
    ) {}

    public static record Sky(float defaultZenithAngle) {}

    public final ResourceLocation location;
    public final Map<Option.Element<?>, Object> appliedOptions;

    public final boolean smoothBrightnessBidirectionaly;
    public final int brightnessSmoothingFrames;  // I wonder why smoothing is frame dependent, not time?
    public final int rainSmoothingFrames;
    public final int thunderSmoothingFrames;

    public final @Nullable SkyShadows skyShadows;
    public final @Nullable Sky sky;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentItemEntityFramebuffer;
    public final Framebuffer particlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    public final Map<VertexFormat, MaterialProgram> materialPrograms;
    public final Map<VertexFormat, MaterialProgram> shadowMaterialPrograms;

    // private
    private final Map<String, Program> programs = new HashMap<>();
    private final Map<Pair<ResourceLocation, Type>, Shader> shaders = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Framebuffer> framebuffers = new HashMap<>();

    private final PassBase[]
        onInitPasses,
        beforeWorldRenderPasses,
        fabulousPasses,
        afterRenderHandPasses,
        onResizePasses;
    private boolean runInitPasses = true;
    private boolean runResizePasses = true;

    public Pipeline(PipelineRaw rawPipeline, Map<Option.Element<?>, Object> appliedOptions) { try {
        this.location = rawPipeline.location;
        this.appliedOptions = Collections.unmodifiableMap(appliedOptions);

        JsonObject pipelineJson = rawPipeline.json.clone();
        var options = rawPipeline.options;

        Function<String, Option.Element<?>> optionElementByName = (String name) -> {
            for (var o : options.values()) {
                if (o.elements.containsKey(name)) {
                    return o.elements.get(name);
                }
            }
            return null;
        };

        Function<String, Object> optionValueByName = (String name) -> {
            var element = optionElementByName.apply(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        class ApplyOptions { static JsonElement doApply(JsonElement e, Function<String, Object> optionValueByName) {
            if (e instanceof JsonObject vo) {
                if (vo.size() == 1 && vo.containsKey("option")) {
                    String optionName = vo.get(String.class, "option");
                    return new JsonPrimitive(optionValueByName.apply(optionName));
                }
                if (vo.size() == 2 && vo.containsKey("default")) {
                    if (vo.containsKey("option")) {
                        String optionElementName = (String) ((JsonPrimitive) vo.get("option")).getValue();
                        var value = optionValueByName.apply(optionElementName);
                        if (value != null) {
                            return new JsonPrimitive(value);
                        }
                    }
                    if (vo.containsKey("optionMap")) {
                        JsonObject optionO = (JsonObject) vo.get("optionMap");
                        String optionElementName = optionO.keySet().iterator().next();
                        var value = optionValueByName.apply(optionElementName);
                        if (value != null) {
                            for (JsonObject variant : JanksonUtils.listOfObjects(optionO, optionElementName)) {
                                if (variant.get(String.class, "from").equals(value)) {
                                    return (JsonPrimitive) variant.get("to");
                                }
                            }
                        }
                    }
                    return (JsonPrimitive) vo.get("default");
                }
                for (var kv : vo.entrySet()) {
                    kv.setValue(doApply(kv.getValue(), optionValueByName));
                }
            }
            if (e instanceof JsonArray va) {
                for (int i = 0; i < va.size(); ++i) {
                    va.set(i, doApply(va.get(i), optionValueByName));
                }
            }
            return e;
        }}
        ApplyOptions.doApply(pipelineJson, optionValueByName);

        this.smoothBrightnessBidirectionaly = pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false);
        this.brightnessSmoothingFrames = pipelineJson.getInt("brightnessSmoothingFrames", 20);
        this.rainSmoothingFrames = pipelineJson.getInt("rainSmoothingFrames", 500);
        this.thunderSmoothingFrames = pipelineJson.getInt("thunderSmoothingFrames", 500);

        JsonObject skyJson = pipelineJson.getObject("sky");
        if (skyJson != null) {
            this.sky = new Sky(
                (float) Math.toRadians(skyJson.getFloat("defaultZenithAngle", 0.0F))
            );
        }
        else {
            this.sky = null;
        }

        // "images"
        Function<String, Optional<Texture>> getOrLoadOptionalTexture = (String name) -> {
            return Optional.ofNullable(this.textures.computeIfAbsent(name, _name -> {
                List<JsonObject> textures = JanksonUtils.listOfObjects(pipelineJson, "images");
                Optional<JsonObject> possibleJson = textures.stream().filter(t -> t.get(String.class, "name").equals(name)).findFirst();
                if (possibleJson.isEmpty()) {
                    return null;
                }
                return Texture.load(possibleJson.get(), location);
            }));
        };

        Function<String, Texture> getOrLoadTexture = (String name) -> {
            var result = getOrLoadOptionalTexture.apply(name);
            if (result.isEmpty()) {
                throw new RuntimeException("Couldn't find texture \""+name+"\"");
            }
            return result.get();
        };

        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture = (String name) -> {
            if (name.contains(":")) {
                var mc = Minecraft.getInstance();
                var rl = ResourceLocation.parse(name);
                // compat, was changed in resource pack format v13
                if (rl.equals(ResourceLocation.withDefaultNamespace("textures/misc/enchanted_item_glint.png"))) {
                    rl = ItemRenderer.ENCHANTED_GLINT_ITEM;
                }
                return Optional.of(mc.getTextureManager().getTexture(rl));
            }
            else {
                return Optional.of(getOrLoadOptionalTexture.apply(name).orElse(null));
            }
        };

        // "framebuffers"
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer = (String name) -> {
            return Optional.ofNullable(this.framebuffers.computeIfAbsent(name, _name -> {
                try {
                    List<JsonObject> framebuffers = JanksonUtils.listOfObjects(pipelineJson, "framebuffers");
                    Optional<JsonObject> possibleJson = framebuffers.stream().filter(t -> t.get(String.class, "name").equals(name)).findFirst();
                    if (possibleJson.isEmpty()) {
                        return null;
                    }
                    return Framebuffer.load(possibleJson.get(), location, getOrLoadTexture);
                }
                catch (Exception e) {
                    throw new RuntimeException("Error occured when tried to load framebuffer \""+name+"\"", e);
                }
            }));
        };

        Function<String, Framebuffer> getOrLoadFramebuffer = (String name) -> {
            var result = getOrLoadOptionalFramebuffer.apply(name);
            if (result.isEmpty()) {
                throw new RuntimeException("Couldn't find framebuffer \""+name+"\"");
            }
            return result.get();
        };

        JsonObject skyShadowsO = pipelineJson.getObject("skyShadows");
        if (skyShadowsO != null) {
            this.skyShadows = new SkyShadows(
                getOrLoadFramebuffer.apply(skyShadowsO.get(String.class, "framebuffer")),
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

        Framebuffer shadowFramebuffer = this.skyShadows != null ? this.skyShadows.framebuffer : null;

        JsonObject targetsO = pipelineJson.getObject("drawTargets");

        this.defaultFramebuffer = getOrLoadFramebuffer.apply(pipelineJson.get(String.class, "defaultFramebuffer"));
        this.solidFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "translucentTerrain"));
        this.translucentItemEntityFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "translucentEntity"));
        this.particlesFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "weather"));
        this.cloudsFramebuffer = getOrLoadFramebuffer.apply(targetsO.get(String.class, "clouds"));

        Map<ResourceLocation, String> shaderSourceCache = new HashMap<>();

        Function<ResourceLocation, Optional<String>> getShaderSource = (ResourceLocation location) -> {
            String source = shaderSourceCache.computeIfAbsent(location, (loc) -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    var resource = mc.getResourceManager().getResource(location);
                    if (resource.isEmpty()) {
                        return null;
                    }
                    return IOUtils.toString(resource.get().openAsReader());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return Optional.ofNullable(source);
        };

        // "programs"
        int glslVersion = pipelineJson.getInt("glslVersion", 330);

        BiFunction<ResourceLocation, Type, Shader> getOrLoadShader = (ResourceLocation location, Type type) -> {
            return this.shaders.computeIfAbsent(Pair.of(location, type), locationAndType -> {
                String source = getShaderSource.apply(location).get();
                return Shader.load(location, source, type, glslVersion, options, appliedOptions, getShaderSource, shadowFramebuffer);
            });
        };

        Function<String, Program> getOrLoadProgram = (String name) -> {
            return this.programs.computeIfAbsent(name, _name -> {
                List<JsonObject> programs = JanksonUtils.listOfObjects(pipelineJson, "programs");
                JsonObject programO = programs.stream().filter(program -> program.get(String.class, "name").equals(name)).findFirst().get();
                return Program.load(programO, location, getOrLoadShader);
            });
        };

        // "materialProgram"
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        JsonObject materailProgram = pipelineJson.getObject("materialProgram");

        var materialVertexShaderLocation = ResourceLocation.parse(materailProgram.get(String.class, "vertexSource"));
        var materialFragmentShaderLocation = ResourceLocation.parse(materailProgram.get(String.class, "fragmentSource"));

        List<String> samplers = JanksonUtils.listOfStrings(materailProgram, "samplers");
        List<Optional<? extends AbstractTexture>> samplerImages = new ArrayList<>() {{
            for (String textureName : JanksonUtils.listOfStrings(materailProgram, "samplerImages")) {
                add(getOrLoadPipelineOrResourcepackTexture.apply(textureName));
            }
        }};

        var vertexFormats = new VertexFormat[] {
            CanPipe.VertexFormats.BLOCK,
            CanPipe.VertexFormats.NEW_ENTITY,
            CanPipe.VertexFormats.PARTICLE
        };
        this.materialPrograms = Stream.of(vertexFormats).collect(Collectors.toUnmodifiableMap(
            vertexFormat -> vertexFormat,
            vertexFormat -> MaterialProgram.load(
                vertexFormat, glslVersion, enablePBR, false, shadowFramebuffer,
                location, materialVertexShaderLocation, materialFragmentShaderLocation,
                options, appliedOptions, samplers, samplerImages, getShaderSource
            )
        ));

        if (this.skyShadows != null) {
            this.shadowMaterialPrograms = Stream.of(vertexFormats).collect(Collectors.toUnmodifiableMap(
                vertexFormat -> vertexFormat,
                vertexFormat -> MaterialProgram.load(
                    vertexFormat, glslVersion, enablePBR, true /* depth pass */, shadowFramebuffer,
                    location, this.skyShadows.vertexShaderLocation, this.skyShadows.fragmentShaderLocation,
                    options, appliedOptions, List.of(), List.of(), getShaderSource
                )
            ));
        } else {
            this.shadowMaterialPrograms = Map.of();
        }

        // passes
        Function<String, PassBase[]> loadPasses = (name) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            List<PassBase> result = new ArrayList<>();
            if (passesJson != null) {
                for (var passO : JanksonUtils.listOfObjects(passesJson, "passes")) {
                    Pass.load(
                        passO, optionValueByName,
                        getOrLoadOptionalFramebuffer,
                        getOrLoadProgram,
                        getOrLoadPipelineOrResourcepackTexture
                    ).ifPresent(pass -> result.add(pass));
                }
            }
            return result.toArray(new PassBase[]{});
        };

        this.onInitPasses = loadPasses.apply("onInit");
        this.onResizePasses = loadPasses.apply("onResize");
        this.beforeWorldRenderPasses = loadPasses.apply("beforeWorldRender");
        this.fabulousPasses = loadPasses.apply("fabulous");
        this.afterRenderHandPasses = loadPasses.apply("afterRenderHand");
    } catch (Exception e) {
        this.close();
        throw e;
    }}

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> t.onWindowSizeChanged(w, h));
        this.framebuffers.forEach((n, f) -> f.resize(w, h));
        this.runResizePasses = true;
    }

    public void onBeforeWorldRender(Matrix4f view, Matrix4f projection) {
        var mc = Minecraft.getInstance();

        Stream.of(
            this.materialPrograms.values().stream(),
            this.shadowMaterialPrograms.values().stream(),
            this.programs.values().stream()
        ).flatMap(p -> p).forEach(p -> {
            p.setDefaultUniforms(Mode.QUADS, view, projection, mc.getWindow());
            p.setFREXUniforms();
        });

        if (this.runInitPasses) {
            for (PassBase pass : this.onInitPasses) {
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
    }

    public void onAfterWorldRender(Matrix4f view, Matrix4f projection) {
        for (PassBase pass : this.fabulousPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);

        for (MaterialProgram p : this.materialPrograms.values()) {
            GlStateManager._glUseProgram(p.getProgramId());
            if (p.CANPIPE_ORIGIN_TYPE != null) {
                p.CANPIPE_ORIGIN_TYPE.set(3);  // hand
                p.CANPIPE_ORIGIN_TYPE.upload();
            }
            if (p.FRX_MODEL_TO_WORLD != null) {
                p.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F, 1.0F);
                p.FRX_MODEL_TO_WORLD.upload();
            }
        }
        GlStateManager._glUseProgram(0);
    }

    public void onAfterRenderHand(Matrix4f view, Matrix4f projection) {
        for (PassBase pass : this.afterRenderHandPasses) {
            pass.apply(view, projection);
        }

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, mc.getMainRenderTarget().width, mc.getMainRenderTarget().height);

        for (MaterialProgram p : this.materialPrograms.values()) {
            GlStateManager._glUseProgram(p.getProgramId());
            if (p.CANPIPE_ORIGIN_TYPE != null) {
                p.CANPIPE_ORIGIN_TYPE.set(2);  // screen
                p.CANPIPE_ORIGIN_TYPE.upload();
            }
        }
        GlStateManager._glUseProgram(0);
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result, float partialTicks) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = level.getSunAngle(partialTicks);
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
        this.framebuffers.values().forEach(Framebuffer::close);
        this.textures.values().forEach(Texture::close);
        this.shaders.values().forEach(Shader::close);
        this.programs.values().forEach(ProgramBase::close);
    }

}
