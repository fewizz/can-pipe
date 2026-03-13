package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.MinecraftExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record Shadows(
        Map<RenderPipeline, RenderPipeline> materialPrograms,
        List<Framebuffer> framebuffers,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits,
        boolean supportForwardRender,  // isn't used in canvas
        boolean allowEntities,
        boolean allowParticles  // isn't used in canvas
    ) {}

    public final Identifier location;
    public final Map<Option.Element<?>, Object> appliedOptions;

    public final float defaultZenithAngle;
    public final boolean smoothBrightnessBidirectionaly;
    public final int brightnessSmoothingFrames;  // I wonder why smoothing is frame dependent, not time?
    public final int rainSmoothingFrames;
    public final int thunderSmoothingFrames;

    public final Framebuffer defaultFramebuffer;
    public final Framebuffer solidFramebuffer;
    public final Framebuffer translucentTerrainFramebuffer;
    public final Framebuffer translucentItemEntityFramebuffer;
    public final Framebuffer particlesFramebuffer;
    public final Framebuffer weatherFramebuffer;
    public final Framebuffer cloudsFramebuffer;

    public final @Nullable Shadows shadows;

    private final Map<RenderPipeline, RenderPipeline> materialPrograms;
    private final Map<String, ? extends AbstractTexture> materialProgramSamplerTextures;

    private final Map<String, RenderPipeline> programs = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Framebuffer> framebuffers = new HashMap<>();

    private final List<PassBase>
        onInitPasses = new ArrayList<>(),
        beforeWorldRenderPasses = new ArrayList<>(),
        fabulousPasses = new ArrayList<>(),
        afterRenderHandPasses = new ArrayList<>(),
        onResizePasses = new ArrayList<>();

    Pipeline(PipelineRaw rawPipeline, Map<Option.Element<?>, Object> appliedOptions) { try {
        this.location = rawPipeline.location;
        this.appliedOptions = Collections.unmodifiableMap(appliedOptions);

        JsonObject pipelineJson = rawPipeline.getPipelineJson(appliedOptions);

        /* Manually fixing some shaderpacks here */

        // https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5
        if (this.location.getNamespace().contains("forgetmenot")) {
            var programs = pipelineJson.get(JsonArray.class, "programs");
            if (programs != null) {
                programs.stream().filter(
                    (JsonElement program) ->
                        program instanceof JsonObject programJson &&
                        programJson.containsKey("name") &&
                        programJson.get(String.class, "name").equals("depth_downsample")
                ).findFirst().ifPresent(program -> {
                    JsonObject programJson = (JsonObject) program;
                    JsonArray samplers = programJson.get(JsonArray.class, "samplers");
                    if (samplers.size() != 1 || !(samplers.get(0) instanceof JsonPrimitive)) {
                        return;
                    }
                    JsonPrimitive sampler = (JsonPrimitive) samplers.get(0);
                    if (sampler.asString().equals("u_depth")) {
                        CanPipe.LOGGER.warn("replacing sampler \"u_depth\" with \"u_depth_mips\" for program \"depth_downsample\"");
                        samplers.set(0, JsonPrimitive.of("u_depth_mips"));
                    }
                });
            }
        }

        // https://github.com/ambrosia13/Aerie-Shaders/pull/2
        if (this.location.getNamespace().contains("aerie")) {
            var programs = pipelineJson.get(JsonArray.class, "programs");
            if (programs != null) {
                programs.stream().filter(
                    (JsonElement program) ->
                        program instanceof JsonObject programJson &&
                        programJson.containsKey("name") &&
                        programJson.get(String.class, "name").equals("copy")
                ).findFirst().ifPresent(program -> {
                    JsonObject programJson = (JsonObject) program;
                    JsonArray samplers = programJson.get(JsonArray.class, "samplers");
                    if (samplers.size() != 1 || !(samplers.get(0) instanceof JsonPrimitive)) {
                        return;
                    }
                    JsonPrimitive sampler = (JsonPrimitive) samplers.get(0);
                    if (sampler.asString().equals("u_composite")) {
                        CanPipe.LOGGER.warn("replacing sampler \"u_composite\" with \"u_color\" for program \"copy\"");
                        samplers.set(0, JsonPrimitive.of("u_color"));
                    }
                });
            }
        }

        /* End of shaderpacks fixing */

        var options = rawPipeline.options;

        Function<String, Object> optionValueByName = (String name) -> {
            var element = rawPipeline.optionElementByName(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        this.defaultZenithAngle = (float) Math.toRadians(JanksonUtils.objectOrEmpty(pipelineJson, "sky").getFloat("defaultZenithAngle", 0.0F));
        this.smoothBrightnessBidirectionaly = pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false);
        this.brightnessSmoothingFrames = pipelineJson.getInt("brightnessSmoothingFrames", 20);
        this.rainSmoothingFrames = pipelineJson.getInt("rainSmoothingFrames", 500);
        this.thunderSmoothingFrames = pipelineJson.getInt("thunderSmoothingFrames", 500);

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
            AbstractTexture texture = null;
            if (name.contains(":")) {
                var mc = Minecraft.getInstance();
                var id = CanPipe.upgradeResourcePath(Identifier.parse(name));

                if (id.equals(Identifier.withDefaultNamespace("textures/environment/moon_phases.png"))) {
                    texture = new MoonPhasesTexture();
                }
                if (texture == null) {
                    texture = mc.getTextureManager().getTexture(id);
                }
            }
            else {
                texture = getOrLoadOptionalTexture.apply(name).orElse(null);
            }
            return Optional.ofNullable(texture);
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

        JsonObject targetsJson = pipelineJson.getObject("drawTargets");

        this.defaultFramebuffer = getOrLoadFramebuffer.apply(pipelineJson.get(String.class, "defaultFramebuffer"));
        if (this.defaultFramebuffer.colorTextures.length != 1) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" has "+this.defaultFramebuffer.colorTextures.length+" color attachments, should have only one");
        }
        if (this.defaultFramebuffer.getDepthTexture() == null) {
            throw new RuntimeException("Default framebuffer \""+this.defaultFramebuffer.name+"\" doesn't have depth attachment");
        }

        this.solidFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "solidTerrain"));
        this.translucentTerrainFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentTerrain"));
        this.translucentItemEntityFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentEntity"));
        this.particlesFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentParticles"));
        this.weatherFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "weather"));
        this.cloudsFramebuffer = getOrLoadFramebuffer.apply(targetsJson.get(String.class, "clouds"));

        Map<Identifier, String> shaderSourceCache = new HashMap<>();

        Function<Identifier, Optional<String>> getShaderSource = (Identifier location) -> {
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

        // "materialProgram"
        boolean enablePBR = pipelineJson.getBoolean("enablePBR", false);
        int glslVersion = pipelineJson.getInt("glslVersion", 330);

        var renderPipelines = new RenderPipeline[] {
            RenderPipelines.SOLID_BLOCK,
            RenderPipelines.SOLID_TERRAIN,
            RenderPipelines.CUTOUT_BLOCK,
            RenderPipelines.CUTOUT_TERRAIN,
            RenderPipelines.TRANSLUCENT_TERRAIN,
            RenderPipelines.BEACON_BEAM_OPAQUE,
            RenderPipelines.BEACON_BEAM_TRANSLUCENT,
            RenderPipelines.TRANSLUCENT_BLOCK,

            RenderPipelines.ARMOR_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_TRANSLUCENT,

            RenderPipelines.ENTITY_SOLID,
            RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD,
            RenderPipelines.ENTITY_CUTOUT,
            RenderPipelines.ENTITY_CUTOUT_CULL,
            RenderPipelines.ENTITY_CUTOUT_Z_OFFSET,
            RenderPipelines.ENTITY_CUTOUT_DISSOLVE,
            RenderPipelines.ENTITY_TRANSLUCENT,
            RenderPipelines.ENTITY_TRANSLUCENT_CULL,
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,

            RenderPipelines.ITEM_CUTOUT,
            RenderPipelines.ITEM_TRANSLUCENT,

            RenderPipelines.EYES,

            RenderPipelines.LEASH,

            RenderPipelines.OPAQUE_PARTICLE,
            RenderPipelines.TRANSLUCENT_PARTICLE
        };

        Framebuffer shadowFramebuffer = null;
        JsonObject shadowsJson = pipelineJson.getObject("skyShadows");

        if (shadowsJson != null) {
            shadowFramebuffer = getOrLoadFramebuffer.apply(shadowsJson.get(String.class, "framebuffer"));
        }

        Optional<Integer> shadowMapSize = (
            shadowFramebuffer != null ?
            Optional.of(shadowFramebuffer.getDepthTexture().getWidth(0)) :
            Optional.empty()
        );

        JsonObject materialProgram = pipelineJson.getObject("materialProgram");

        var materialVertexShaderLocation = Identifier.parse(materialProgram.get(String.class, "vertexSource"));
        var materialFragmentShaderLocation = Identifier.parse(materialProgram.get(String.class, "fragmentSource"));

        List<String> samplers = new ArrayList<>(JanksonUtils.listOfStrings(materialProgram, "samplers"));
        if (shadowFramebuffer != null) {
            samplers.add("frxs_shadowMap");
            samplers.add("frxs_shadowMapTexture");
        }
        this.materialPrograms = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
            renderPipeline -> renderPipeline,
            renderPipeline -> MaterialPrograms.load(
                renderPipeline, glslVersion, enablePBR, false, shadowMapSize,
                materialVertexShaderLocation, materialFragmentShaderLocation,
                options, appliedOptions, samplers, getShaderSource,
                0.0F, 0.0F
            )
        ));

        var samplerImagesNames = JanksonUtils.listOfStrings(materialProgram, "samplerImages");
        Map<String, AbstractTexture> samplerToTexture = new HashMap<>();
        for (int i = 0; i < Math.min(samplers.size(), samplerImagesNames.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerImage = getOrLoadPipelineOrResourcepackTexture.apply(samplerImagesNames.get(i)).get();
            samplerToTexture.put(sampler, samplerImage);
        }
        if (shadowFramebuffer != null) {
            String shadowMapTextureName = shadowFramebuffer.getDepthTexture().getLabel();
            AbstractTexture shadowMapTexture = getOrLoadPipelineOrResourcepackTexture.apply(shadowMapTextureName).get();
            samplerToTexture.put("frxs_shadowMap", shadowMapTexture);
            samplerToTexture.put("frxs_shadowMapTexture", shadowMapTexture);
        }
        this.materialProgramSamplerTextures = samplerToTexture;

        if (shadowsJson != null) {
            // Instead of one shadow framebuffer, we create N (= number of cascades) framebuffers for different layers
            List<Framebuffer> framebuffers = new ArrayList<>();
            int baseArrayLayer = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer();
            int layerCount = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_layerCount();
            var cascadeRadii = JanksonUtils.listOfIntegers(shadowsJson, "cascadeRadius");
            for (int i = 0; i < cascadeRadii.size() + 1; ++i) {
                final int cascade = i;
                GpuTexture shadowMapTexture = shadowFramebuffer.getDepthTexture();
                GpuTextureView shadowMapTextureView = shadowFramebuffer.getDepthTextureView();
                Framebuffer fb = new Framebuffer(
                    location,
                    shadowFramebuffer.name+"_"+(cascade+1),
                    (idx) -> { throw new RuntimeException("Color texture getter should not be called"); },
                    new int[]{},  // clear colors
                    () -> {
                        var shadowMapCascadeTextureView = ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_createTextureView(
                            shadowMapTexture,
                            shadowMapTextureView.baseMipLevel(),
                            shadowMapTextureView.mipLevels(),
                            baseArrayLayer + layerCount * cascade,
                            layerCount
                        );
                        return Pair.of(shadowMapTexture, shadowMapCascadeTextureView);
                    },
                    shadowFramebuffer.depthTextureClearDepth
                );
                framebuffers.add(fb);
                this.framebuffers.put(fb.name, fb);
            }

            var vertexShaderLocation = Identifier.parse(shadowsJson.get(String.class, "vertexSource"));
            var fragmentShaderLocation = Identifier.parse(shadowsJson.get(String.class, "fragmentSource"));
            var materialPrograms = Stream.of(renderPipelines).collect(Collectors.toUnmodifiableMap(
                renderPipeline -> renderPipeline,
                renderPipeline -> MaterialPrograms.load(
                    renderPipeline, glslVersion, enablePBR, true, shadowMapSize,
                    vertexShaderLocation, fragmentShaderLocation, options, appliedOptions,
                    List.of(),
                    getShaderSource,
                    shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                    shadowsJson.getFloat("offsetBiasUnits", 4.0F)
                )
            ));
            this.shadows = new Shadows(
                materialPrograms,
                framebuffers,
                cascadeRadii,
                shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                shadowsJson.getFloat("offsetBiasUnits", 4.0F),
                shadowsJson.getBoolean("supportForwardRender", true),
                shadowsJson.getBoolean("allowEntities", true),
                shadowsJson.getBoolean("allowParticles", true)
            );
        }
        else {
            this.shadows = null;
        }

        // "programs"
        Function<String, RenderPipeline> getOrLoadProgram = (String name) -> {
            return this.programs.computeIfAbsent(name, _name -> {
                List<JsonObject> programs = JanksonUtils.listOfObjects(pipelineJson, "programs");
                JsonObject programJson = programs.stream().filter(program -> program.get(String.class, "name").equals(name)).findFirst().get();
                return Programs.load(
                    programJson, location, getShaderSource, glslVersion,
                    options, appliedOptions, shadowMapSize
                );
            });
        };

        // passes
        BiConsumer<String, List<PassBase>> loadPasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson != null) {
                for (var passJson : JanksonUtils.listOfObjects(passesJson, "passes")) {
                    Pass.load(
                        passJson, optionValueByName,
                        getOrLoadOptionalFramebuffer,
                        getOrLoadProgram,
                        getOrLoadPipelineOrResourcepackTexture
                    ).ifPresent(passes::add);
                }
            }
        };

        loadPasses.accept("onInit", this.onInitPasses);
        loadPasses.accept("onResize", this.onResizePasses);
        loadPasses.accept("beforeWorldRender", this.beforeWorldRenderPasses);
        loadPasses.accept("fabulous", this.fabulousPasses);
        loadPasses.accept("afterRenderHand", this.afterRenderHandPasses);
    } catch (Exception e) {
        this.close();
        throw e;
    }}

    @Override
    public void close() {
        this.onInitPasses.forEach(PassBase::close);
        this.onResizePasses.forEach(PassBase::close);
        this.beforeWorldRenderPasses.forEach(PassBase::close);
        this.fabulousPasses.forEach(PassBase::close);
        this.afterRenderHandPasses.forEach(PassBase::close);

        this.framebuffers.values().forEach(Framebuffer::destroyBuffers);
        this.textures.values().forEach(Texture::close);
    }

    public void onWindowSizeChanged(int w, int h) {
        this.textures.forEach((n, t) -> t.onWindowSizeChanged());
        this.framebuffers.forEach((n, f) -> f.onWindowSizeChanged());
    }

    public void onBeforeRenderingLevel(Matrix4fc view, Matrix4fc projection, boolean runResizePasses, boolean runInitPasses) {
        Profiler.get().push("can-pipe before world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        if (runInitPasses) {
            for (PassBase pass : this.onInitPasses) {
                pass.apply(commandEncoder);
            }
        }

        if (runResizePasses) {
            for (PassBase pass : this.onResizePasses) {
                pass.apply(commandEncoder);
            }
        }

        for (PassBase pass : this.beforeWorldRenderPasses) {
            pass.apply(commandEncoder);
        }

        ((MinecraftExtended) Minecraft.getInstance()).canpipe_setMainRenderTargetOverride(this.solidFramebuffer);

        Profiler.get().pop();
    }

    public void onAfterWorldRender() {
        Profiler.get().push("can-pipe after world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (PassBase pass : this.fabulousPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterRenderHand() {
        Profiler.get().push("can-pipe after hand");

        ((MinecraftExtended) Minecraft.getInstance()).canpipe_setMainRenderTargetOverride(this.defaultFramebuffer);

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (PassBase pass : this.afterRenderHandPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public RenderPipeline getReplaceRenderPipeline(RenderPipeline renderPipeline) {
        Minecraft mc = Minecraft.getInstance();
        if (this.shadows != null && ((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
            renderPipeline = this.shadows.materialPrograms().getOrDefault(renderPipeline, renderPipeline);
        }
        else {
            renderPipeline = this.materialPrograms.getOrDefault(renderPipeline, renderPipeline);
        }
        return renderPipeline;
    }

    public RenderPass createRenderPass(CommandEncoder commandEncoder, Supplier<String> name, Framebuffer framebuffer) {
        GameRendererExtended gre = (GameRendererExtended) Minecraft.getInstance().gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;

        RenderPass renderPass;
        // For example, when rendering gui items
        if (RenderSystem.outputColorTextureOverride != null && RenderSystem.outputDepthTextureOverride != null) {
            renderPass = commandEncoder.createRenderPass(name, RenderSystem.outputColorTextureOverride, OptionalInt.empty(), RenderSystem.outputDepthTextureOverride, OptionalDouble.empty());
        }
        else {
            renderPass = ((CommandEncoderExtended) commandEncoder).canpipe_createRenderPass(name, framebuffer.colorTextureViews, framebuffer.getDepthTextureView());
        }

        renderPass.setUniform("frx_ub_accessibility", Uniforms.ACCESSIBILITY_UBO);
        renderPass.setUniform("frx_ub_view", Uniforms.VIEW_UBO);
        renderPass.setUniform("frx_ub_shadow", Uniforms.SHADOW_UBO);
        renderPass.setUniform("frx_ub_player", Uniforms.PLAYER_UBO);
        renderPass.setUniform("frx_ub_world", Uniforms.WORLD_UBO);
        renderPass.setUniform("frx_ub_fog", Uniforms.FOG_UBO);

        renderPass.setUniform("frxu_ub_cascade", Uniforms.INT_0_3_UBO_BUFFERS[lre.canpipe_getShadowCascade()]);
        renderPass.setUniform("canpipe_ub_origin_type", Uniforms.INT_0_3_UBO_BUFFERS[gre.canpipe_getOriginType()]);
        renderPass.setUniform("canpipe_ub_is_rendering_hand", Uniforms.INT_0_3_UBO_BUFFERS[gre.canpipe_isRenderingHand() ? 1 : 0]);

        int renderTarget = 0;
        if (framebuffer == this.translucentTerrainFramebuffer) {
            renderTarget = 1;
        }
        if (framebuffer == this.translucentItemEntityFramebuffer) {
            renderTarget = 2;
        }
        if (framebuffer == this.particlesFramebuffer) {
            renderTarget = 3;
        }

        renderPass.setUniform("canpipe_ub_render_target", Uniforms.INT_0_3_UBO_BUFFERS[renderTarget]);

        renderPass.bindTexture("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));

        for (var e : this.materialProgramSamplerTextures.entrySet()) {
            AbstractTexture texture = e.getValue();
            renderPass.bindTexture(e.getKey(), texture.getTextureView(), texture.getSampler());
        }

        return renderPass;
    }

    public static void bindSpritesExtentsSampler(RenderPass renderPass, GpuTextureView sampler0) {
        var mc = Minecraft.getInstance();
        MutableObject<TextureAtlas> atlas = new MutableObject<>();

        mc.getAtlasManager().forEach((loc, possibleAtlas) -> {
            if (atlas.get() == null && possibleAtlas.getTexture() == sampler0.texture()) {
                atlas.setValue(possibleAtlas);
            }
        });
        if (atlas.get() == null) {  // we just need to bind something
            atlas.setValue(mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
        }
        renderPass.setUniform(
            "canpipe_spritesExtents",
            ((TextureAtlasExtended) atlas.get()).canpipe_getSpritesExtentsBuffer()
        );
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.skyRenderState.sunAngle;
        long ticks = level.getDefaultClockTime() % 24000L;

        result.set(
            (float) (-Math.sin(hourAngle)),
            (float) ( Math.cos(hourAngle) *  Math.cos(this.defaultZenithAngle)),
            (float) ( Math.cos(hourAngle) * -Math.sin(this.defaultZenithAngle))
        );

        if (ticks > 13000L && ticks < 23000L) {  // moon
            result.negate();
        }

        return result;
    }

}
