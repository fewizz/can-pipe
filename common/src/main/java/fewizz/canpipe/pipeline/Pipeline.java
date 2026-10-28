package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.renderpearl.GpuDeviceExtended;
import fewizz.canpipe.renderpearl.GpuTextureViewExtended;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record Targets(
        Framebuffer main,
        Framebuffer solid,
        Framebuffer translucentTerrain,
        Framebuffer translucentItemEntity,
        Framebuffer translucentParticles,
        Framebuffer weather,
        Framebuffer clouds
    ) {}

    public static record FabulousTargets(
        Framebuffer translucentTerrain,
        Framebuffer translucentItemEntity,
        Framebuffer translucentParticles,
        Framebuffer weather,
        Framebuffer clouds
    ) {}

    public static record Shadows(
        Map<RenderPipeline, MaterialProgramLoader> materialProgramsLoaders,
        List<Framebuffer> framebuffers,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits,
        boolean supportForwardRender,  // isn't used in canvas
        boolean allowEntities,
        boolean allowParticles  // isn't used in canvas
    ) {}

    public static record Settings(
        float defaultZenithAngle,
        boolean smoothBrightnessBidirectionally,
        int brightnessSmoothingFrames,  // I wonder why smoothing is frame dependent, not time?
        int rainSmoothingFrames,
        int thunderSmoothingFrames,
        boolean awareOfDepthRangeChanges
    ) {}

    public final Identifier location;
    public final Map<OptionGroup.Element<?>, Object> appliedOptions;

    public final Settings settings;
    public final Targets targets;
    public final @Nullable FabulousTargets fabulousTargets;
    public final @Nullable Shadows shadows;

    private final Map<RenderPipeline, MaterialProgramLoader> materialProgramsLoaders = new HashMap<>();
    private final Map<String, ? extends AbstractTexture> materialProgramSamplerTextures;

    private final Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, Pair<RenderPipeline, CompletableFuture<CompiledRenderPipeline>>> programs = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private final Map<String, Framebuffer> framebuffers = new HashMap<>();

    public final Map<Pair<List<GpuFormat>, RenderPipeline>, CompiledRenderPipeline> replacedRenderPipelines = new HashMap<>();

    private final List<Pass>
        onInitPasses = new ArrayList<>(),
        beforeWorldRenderPasses = new ArrayList<>(),
        fabulousPasses = new ArrayList<>(),
        afterRenderHandPasses = new ArrayList<>(),
        onResizePasses = new ArrayList<>();

    Pipeline(PipelineRaw rawPipeline, Map<OptionGroup.Element<?>, Object> appliedOptions) throws Exception { try {
        this.location = rawPipeline.location;
        this.appliedOptions = Collections.unmodifiableMap(appliedOptions);

        Minecraft mc = Minecraft.getInstance();

        JsonObject pipelineJson = rawPipeline.getPipelineJson(appliedOptions);

        PipelinesFixes.fix(this.location, pipelineJson);

        var options = rawPipeline.options;

        Function<String, Object> optionValueByName = (String name) -> {
            var element = rawPipeline.optionElementByName(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        this.settings = new Settings(
            (float) Math.toRadians(JanksonUtils.objectOrEmpty(pipelineJson, "sky").getFloat("defaultZenithAngle", 0.0F)),
            pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false),
            pipelineJson.getInt("brightnessSmoothingFrames", 20),
            pipelineJson.getInt("rainSmoothingFrames", 500),
            pipelineJson.getInt("thunderSmoothingFrames", 500),
            rawPipeline.awareOfDepthRangeChanges
        );

        // "images"
        Function<String, Optional<Texture>> getOrLoadOptionalTexture = (String name) -> {
            return Optional.ofNullable(this.textures.computeIfAbsent(name, _name -> {
                List<JsonObject> textures = JanksonUtils.listOfObjects(pipelineJson, "images");
                Optional<JsonObject> possibleJson = textures.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                if (possibleJson.isEmpty()) {
                    return null;
                }
                try {
                    return Texture.load(possibleJson.get(), location);
                } catch(Exception e) {
                    throw new RuntimeException("Couldn't load texture \""+name+"\"", e);
                }
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
                var id = CanPipe.upgradeIdentifier(Identifier.parse(name));

                if (id.equals(Identifier.withDefaultNamespace("textures/environment/moon_phases.png"))) {
                    texture = textures.computeIfAbsent(id.toString(), k -> new MoonPhasesTexture());
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
                    Optional<JsonObject> possibleJson = framebuffers.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                    if (possibleJson.isEmpty()) {
                        return null;
                    }
                    return Framebuffer.load(possibleJson.get(), location, getOrLoadTexture);
                }
                catch (Exception e) {
                    throw new RuntimeException("Couldn't load framebuffer \""+name+"\"", e);
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

        var defaultFramebuffer = getOrLoadFramebuffer.apply(pipelineJson.get(String.class, "defaultFramebuffer"));
        if (defaultFramebuffer.colorTextures.length != 1) {
            throw new RuntimeException("Default framebuffer \""+defaultFramebuffer.name+"\" has "+defaultFramebuffer.colorTextures.length+" color attachments, should have only one");
        }
        if (defaultFramebuffer.getDepthTexture() == null) {
            throw new RuntimeException("Default framebuffer \""+defaultFramebuffer.name+"\" doesn't have depth attachment");
        }

        JsonObject targetsJson = JanksonUtils.objectOrThrow(pipelineJson, "drawTargets");

        this.targets = new Targets(
            defaultFramebuffer,
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "solidTerrain")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentTerrain")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentEntity")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentParticles")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "weather")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "clouds"))
        );

        JsonObject fabulousTargetsJson = pipelineJson.getObject("fabulousTargets");
        this.fabulousTargets = fabulousTargetsJson == null ? null : new FabulousTargets(
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "translucent")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "entity")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "particles")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "weather")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "clouds"))
        );
        if (pipelineJson.containsKey("fabulous") && this.fabulousTargets == null) {
            throw new RuntimeException("To use \"fabulous\" passes, \"fabulousTargets\" should be defined");
        }

        Map<Identifier, String> shaderSourceCache = new HashMap<>();

        Function<Identifier, Optional<String>> getShaderSource = (Identifier location) -> {
            RenderSystem.assertOnRenderThread();

            String source = shaderSourceCache.computeIfAbsent(location, (loc) -> {
                try {
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
            RenderPipelines.SOLID_BLOCK, RenderPipelines.SOLID_TERRAIN, RenderPipelines.SOLID_TERRAIN_MULTIDRAW,
            RenderPipelines.CUTOUT_BLOCK, RenderPipelines.CUTOUT_TERRAIN, RenderPipelines.CUTOUT_TERRAIN_MULTIDRAW,
            RenderPipelines.TRANSLUCENT_BLOCK, RenderPipelines.TRANSLUCENT_TERRAIN, RenderPipelines.TRANSLUCENT_TERRAIN_MULTIDRAW,
            RenderPipelines.BEACON_BEAM_OPAQUE,
            RenderPipelines.BEACON_BEAM_TRANSLUCENT,

            RenderPipelines.ARMOR_CUTOUT_NO_CULL,
            RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,

            RenderPipelines.ENTITY_SOLID,
            RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD,
            RenderPipelines.ENTITY_CUTOUT_CULL,
            RenderPipelines.ENTITY_CUTOUT,
            RenderPipelines.ENTITY_CUTOUT_Z_OFFSET,
            RenderPipelines.ENTITY_CUTOUT_DISSOLVE,
            RenderPipelines.ENTITY_TRANSLUCENT,
            RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
            RenderPipelines.ENTITY_TRANSLUCENT_CULL,
            RenderPipelines.BANNER_PATTERN,

            RenderPipelines.ITEM_CUTOUT,
            RenderPipelines.ITEM_CUTOUT_GLINT,
            RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL,
            RenderPipelines.ITEM_TRANSLUCENT,
            RenderPipelines.ITEM_TRANSLUCENT_GLINT,
            RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL,

            RenderPipelines.EYES,

            RenderPipelines.LEASH,

            RenderPipelines.OPAQUE_PARTICLE,
            RenderPipelines.TRANSLUCENT_PARTICLE,

            RenderPipelines.TEXT,
            RenderPipelines.TEXT_POLYGON_OFFSET
        };

        Framebuffer shadowFramebuffer = null;
        JsonObject shadowsJson = pipelineJson.getObject("skyShadows");

        if (shadowsJson != null) {
            shadowFramebuffer = getOrLoadFramebuffer.apply(shadowsJson.get(String.class, "framebuffer"));
            Objects.requireNonNull(shadowFramebuffer.getDepthTexture());
            Objects.requireNonNull(shadowFramebuffer.getDepthTextureView());
        }

        Optional<Integer> shadowMapSize = (
            shadowFramebuffer != null ?
            Optional.of(shadowFramebuffer.getDepthTexture().getWidth(0)) :
            Optional.empty()
        );

        JsonObject materialProgram = JanksonUtils.objectOrThrow(pipelineJson, "materialProgram");
        var materialVertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgram, "vertexSource"));
        var materialFragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgram, "fragmentSource"));

        List<String> samplers = new ArrayList<>(JanksonUtils.listOfStrings(materialProgram, "samplers"));
        if (shadowFramebuffer != null) {
            samplers.add("frxs_shadowMap");
            samplers.add("frxs_shadowMapTexture");
        }

        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        Executor localExecutor = runnable -> { tasks.add(runnable); };

        CompletableFuture<Void> materialProgramLoadersAreReady = CompletableFuture.allOf(
            Stream.of(renderPipelines)
            .map(
                renderPipeline -> MaterialPrograms.load(
                    renderPipeline, glslVersion, enablePBR, false, shadowMapSize,
                    materialVertexShaderLocation, materialFragmentShaderLocation,
                    options, appliedOptions, samplers, getShaderSource,
                    0.0F, 0.0F,
                    this.settings.awareOfDepthRangeChanges
                ).whenCompleteAsync(
                    (materialProgramLoader, throwable) -> {
                        if (throwable != null) { throw new RuntimeException(throwable); }
                        this.materialProgramsLoaders.put(materialProgramLoader.originalRenderPipeline(), materialProgramLoader);
                    },
                    localExecutor
                )
            ).toArray(CompletableFuture[]::new)
        );

        var samplerImagesNames = JanksonUtils.listOfStrings(materialProgram, "samplerImages");
        Map<String, AbstractTexture> samplerToTexture = new HashMap<>();
        for (int i = 0; i < Math.min(samplers.size(), samplerImagesNames.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerImage = getOrLoadPipelineOrResourcepackTexture.apply(samplerImagesNames.get(i));
            if (samplerImage.isEmpty()) {
                throw new RuntimeException("Couldn't find material program sampler image \""+samplerImagesNames.get(i)+"\"");
            }
            samplerToTexture.put(sampler, samplerImage.get());
        }
        if (shadowFramebuffer != null) {
            String shadowMapTextureName = shadowFramebuffer.getDepthTexture().getLabel();
            Optional<AbstractTexture> shadowMapTexture = getOrLoadPipelineOrResourcepackTexture.apply(shadowMapTextureName);
            if (shadowMapTexture.isEmpty()) {
                throw new RuntimeException("Couldn't find material program shadowmap image \""+shadowMapTextureName+"\"");
            }
            samplerToTexture.put("frxs_shadowMap", shadowMapTexture.get());
            samplerToTexture.put("frxs_shadowMapTexture", shadowMapTexture.get());
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
                    new Vector4f[]{},  // clear colors
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

            var vertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "vertexSource"));
            var fragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "fragmentSource"));

            this.shadows = new Shadows(
                new HashMap<>(),
                framebuffers,
                cascadeRadii,
                shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                shadowsJson.getFloat("offsetBiasUnits", 4.0F),
                shadowsJson.getBoolean("supportForwardRender", true),
                shadowsJson.getBoolean("allowEntities", true),
                shadowsJson.getBoolean("allowParticles", true)
            );

            materialProgramLoadersAreReady = CompletableFuture.allOf(materialProgramLoadersAreReady, CompletableFuture.allOf(
                Stream.of(renderPipelines)
                .map(
                    renderPipeline -> MaterialPrograms.load(
                        renderPipeline, glslVersion, enablePBR, true, shadowMapSize,
                        vertexShaderLocation, fragmentShaderLocation, options, appliedOptions,
                        List.of(),
                        getShaderSource,
                        shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
                        shadowsJson.getFloat("offsetBiasUnits", 4.0F),
                        this.settings.awareOfDepthRangeChanges
                    ).whenCompleteAsync(
                        (materialProgramLoader, throwable) -> {
                            if (throwable != null) { throw new RuntimeException(throwable); }
                            this.shadows.materialProgramsLoaders.put(materialProgramLoader.originalRenderPipeline(), materialProgramLoader);
                        },
                        localExecutor
                    )
                ).toArray(CompletableFuture[]::new)
            ));
        }
        else {
            this.shadows = null;
        }

        // "programs"

        BiFunction<
            String, Framebuffer,
            Pair<RenderPipeline, CompletableFuture<CompiledRenderPipeline>>
        > getOrLoadProgram = (String name, Framebuffer fb) -> {
            RenderSystem.assertOnRenderThread();

            return this.programs.computeIfAbsent(Pair.of(name, fb.getFormats()), _name -> {
                List<JsonObject> programs = JanksonUtils.listOfObjects(pipelineJson, "programs");
                Optional<JsonObject> programJson = programs.stream().filter(program -> JanksonUtils.stringOrThrow(program, "name").equals(name)).findFirst();
                if (programJson.isEmpty()) {
                    throw new RuntimeException("Couldn't find program \""+name+"\"");
                }
                try {
                    return Programs.load(
                        programJson.get(), location, getShaderSource, glslVersion,
                        options, appliedOptions, shadowMapSize,
                        fb, this.settings.awareOfDepthRangeChanges,
                        localExecutor
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't load program \""+name+"\"", e);
                }
            });
        };

        // passes

        BiFunction<String, List<Pass>, CompletableFuture<Void>> loadPasses = (name, passes) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson == null) { return CompletableFuture.completedFuture(null); }

            List<CompletableFuture<Optional<Pass>>> passessFutures = new ArrayList<>();

            for (var passJson : JanksonUtils.listOfObjects(passesJson, "passes")) {
                passessFutures.add(ProgramPass.load(
                    location,
                    passJson, optionValueByName,
                    getOrLoadOptionalFramebuffer,
                    getOrLoadProgram,
                    getOrLoadPipelineOrResourcepackTexture,
                    localExecutor
                ));
            }

            return Util.sequence(passessFutures).thenAcceptAsync(
                (List<Optional<Pass>> resultingPasses) -> {
                    passes.addAll(
                        resultingPasses.stream()
                            .filter(optional -> optional.isPresent())
                            .map(optional -> optional.get())
                            .toList()
                    );
                },
                localExecutor
            );
        };

        CompletableFuture<Void> passesAreLoaded = CompletableFuture.allOf(
            new CompletableFuture[] {
                loadPasses.apply("onInit", this.onInitPasses),
                loadPasses.apply("onResize", this.onResizePasses),
                loadPasses.apply("beforeWorldRender", this.beforeWorldRenderPasses),
                loadPasses.apply("fabulous", this.fabulousPasses),
                loadPasses.apply("afterRenderHand", this.afterRenderHandPasses)
            }
        );

        while (!passesAreLoaded.isDone() || !materialProgramLoadersAreReady.isDone() || !tasks.isEmpty()) {
            tasks.take().run();
        }

    } catch (Exception e) {
        this.close();
        throw e;
    }}

    @Override
    public void close() {
        this.onInitPasses.forEach(Pass::close);
        this.onResizePasses.forEach(Pass::close);
        this.beforeWorldRenderPasses.forEach(Pass::close);
        this.fabulousPasses.forEach(Pass::close);
        this.afterRenderHandPasses.forEach(Pass::close);

        this.framebuffers.values().forEach(Framebuffer::destroyBuffers);
        this.textures.values().forEach(Texture::close);

        this.programs.values().stream()
            .map(p -> p.getValue())
            .forEach(future -> future.thenAccept(CompiledRenderPipeline::close));

        this.replacedRenderPipelines.values().forEach(CompiledRenderPipeline::close);
        this.materialProgramsLoaders.values().forEach(MaterialProgramLoader::close);
        if (shadows != null) {
            this.shadows.materialProgramsLoaders.values().forEach(MaterialProgramLoader::close);
        }
    }

    public void onWindowSizeChanged() {
        this.textures.values().forEach(Texture::onWindowSizeChanged);
        this.framebuffers.values().forEach(Framebuffer::onWindowSizeChanged);
    }

    public void onBeforeRenderingLevel(boolean runResizePasses, boolean runInitPasses) {
        Profiler.get().push("can-pipe before world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        if (runInitPasses) {
            for (Pass pass : this.onInitPasses) {
                pass.apply(commandEncoder);
            }
        }

        if (runResizePasses) {
            for (Pass pass : this.onResizePasses) {
                pass.apply(commandEncoder);
            }
        }

        for (Pass pass : this.beforeWorldRenderPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterWorldRender() {
        Profiler.get().push("can-pipe after world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.fabulousPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterRenderHand() {
        Profiler.get().push("can-pipe after hand");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.afterRenderHandPasses) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public MaterialProgramLoader getMaterialProgramLoader(RenderPipeline renderPipeline) {
        Minecraft mc = Minecraft.getInstance();

        if (this.shadows != null && ((LevelRendererExtended) mc.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0) {
            return this.shadows.materialProgramsLoaders().get(renderPipeline);
        }
        else {
            return this.materialProgramsLoaders.get(renderPipeline);
        }
    }

    public static void bindSpritesExtentsSampler(RenderPass renderPass, @Nullable GpuTextureView sampler0) {
        var mc = Minecraft.getInstance();
        MutableObject<TextureAtlas> atlas = new MutableObject<>();

        if (sampler0 != null) {
            mc.getAtlasManager().forEach((loc, possibleAtlas) -> {
                if (atlas.get() == null && possibleAtlas.getTexture() == sampler0.texture()) {
                    atlas.setValue(possibleAtlas);
                }
            });
        }

        if (atlas.get() == null) {  // we just need to bind something
            atlas.setValue(mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
        }

        renderPass.setUniform(
            "canpipe_spritesExtents",
            ((TextureAtlasExtended) atlas.get()).canpipe_getSpritesExtentsBuffer()
        );
    }

    public void bindUniforms(RenderPass renderPass, Framebuffer framebuffer, boolean bindVanillaTextures) {
        GameRendererExtended gre = (GameRendererExtended) Minecraft.getInstance().gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;

        Uniforms.setRenderPassFREXUniforms(renderPass);

        renderPass.setUniform("canpipe_ub_origin_type", CanPipe.get0to3UBOBuffers()[gre.canpipe_getOriginType()]);
        renderPass.setUniform("canpipe_ub_is_rendering_hand", CanPipe.get0to3UBOBuffers()[gre.canpipe_isRenderingHand() ? 1 : 0]);
        if (lre.canpipe_getCurrentShadowCascadeIdx() >= 0) {
            renderPass.setUniform("frxu_ub_cascade", CanPipe.get0to3UBOBuffers()[lre.canpipe_getCurrentShadowCascadeIdx()]);
        }

        int renderTarget = 0;
        if (framebuffer == this.targets.translucentTerrain) {
            renderTarget = 1;
        }
        if (framebuffer == this.targets.translucentItemEntity) {
            renderTarget = 2;
        }
        if (framebuffer == this.targets.translucentParticles) {
            renderTarget = 3;
        }

        renderPass.setUniform("canpipe_ub_render_target", CanPipe.get0to3UBOBuffers()[renderTarget]);

        if (bindVanillaTextures) {
            // We don't know yet which render pipeline will be used
            renderPass.setUniform("Sampler0", CanPipe.getWhiteTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.setUniform("Sampler1", Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setUniform("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }

        for (var e : this.materialProgramSamplerTextures.entrySet()) {
            AbstractTexture texture = e.getValue();
            renderPass.setUniform(e.getKey(), texture.getTextureView(), texture.getSampler());
        }
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.skyRenderState.sunAngle;
        long ticks = level.getDefaultClockTime() % 24000L;

        result.set(
            (float) (-Math.sin(hourAngle)),
            (float) ( Math.cos(hourAngle) *  Math.cos(this.settings.defaultZenithAngle)),
            (float) ( Math.cos(hourAngle) * -Math.sin(this.settings.defaultZenithAngle))
        );

        if (ticks > 13000L && ticks < 23000L) {  // moon
            result.negate();
        }

        return result;
    }

    public Framebuffer shadowFramebufferOr(Framebuffer or) {
        int cascade = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx();
        return cascade >= 0 ? this.shadows.framebuffers.get(cascade) : or;
    }

    public CompiledRenderPipeline onRenderPassSetPipeline(RenderPipeline pipeline, List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments) {
        if (!pipeline.getLocation().getNamespace().equals("minecraft")) { return null; }

        boolean patchDepth = !this.settings.awareOfDepthRangeChanges;
        boolean patchAttachment = (
            colorAttachments.size() != pipeline.getColorTargetStates().size() ||
            (!colorAttachments.isEmpty() && colorAttachments.getFirst().textureView().texture().getFormat() != GpuFormat.RGBA8_UNORM)
        );

        if (!patchAttachment && !patchDepth) { return null; }

        var colorFormats = colorAttachments.stream().map(a -> a.textureView().texture().getFormat()).toList();

        return this.replacedRenderPipelines.computeIfAbsent(Pair.of(colorFormats, pipeline), _key -> {
            List<ColorTargetState> colorTargets = pipeline.getColorTargetStates();
            if (patchAttachment) {
                colorTargets = new ArrayList<>();
                for (int idx = 0; idx < colorAttachments.size(); ++idx) {
                    var attachment  = colorAttachments.get(idx);
                    var format = attachment.textureView().texture().getFormat();
                    boolean blend = CanPipe.textureFormatSupportsBlend(format);
                    var colorTargetState = pipeline.getColorTargetStates().get(
                        idx < pipeline.getColorTargetStates().size() ? idx : 0  // questionable
                    );
                    colorTargets.add(new ColorTargetState(
                        blend ? colorTargetState.blendFunction() : Optional.empty(),
                        format,
                        colorTargetState.writeMask()
                    ));
                }
            }

            DepthStencilState depthState = pipeline.getDepthStencilState();
            if (patchDepth && depthState != null) {
                depthState = new DepthStencilState(
                    CanPipe.reverseCompareOp(depthState.depthTest()),
                    depthState.writeDepth(),
                    -depthState.depthBiasScaleFactor(),
                    -depthState.depthBiasConstant()
                );
            }

            Minecraft mc = Minecraft.getInstance();
            Map<Pair<Identifier, ShaderType>, String> shaderSources = new HashMap<>();
            Map<Identifier, ShaderSource.CachedIncludeSource> includeSourcesBuilder = new HashMap<>();

            var defines = pipeline.getShaderDefines();
            if (patchDepth) {
                defines = defines.withOverrides(ShaderDefines.builder().define("CANPIPE_CLIP_DEPTH_RANGE_PATCH").build());
            }

            var newRenderPipeline = new RenderPipeline(
                pipeline.getLocation(),
                pipeline.getShaders(),
                defines,
                pipeline.getBindGroupLayouts(),
                colorTargets.toArray(new ColorTargetState[0]),
                depthState,
                pipeline.getPolygonMode(),
                pipeline.isCull(),
                pipeline.getVertexFormatBindings().toArray(new VertexFormat[0]),
                pipeline.getPrimitiveTopology(),
                pipeline.pushConstantSize(),
                pipeline.getSortKey()
            ) {};

            for (Entry<Identifier, Resource> entry : mc.getResourceManager().listResources("shaders", p -> true).entrySet()) {
                Identifier location = entry.getKey();
                ShaderType shaderType = ShaderType.byLocation(location);
                Resource resource = entry.getValue();

                try {
                    if (shaderType != null) {
                        String contents = resource.readAllAsString();
                        Identifier id = shaderType.idConverter().fileToId(location);
                        shaderSources.put(Pair.of(id, shaderType), contents);
                    }
                    else if (ShaderManager.SHADER_INCLUDE_CONVERTER.matches(location)) {
                        String contents = resource.readAllAsString();
                        Identifier id = ShaderManager.SHADER_INCLUDE_CONVERTER.fileToId(location).withSuffix(ShaderManager.SHADER_INCLUDE_EXTENSION);
                        includeSourcesBuilder.put(id, ShaderSource.CachedIncludeSource.create(id, contents));
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            var device = RenderSystem.getDevice();
            return device.compilePipeline(
                newRenderPipeline,
                new ShaderSource() {

                    @Override
                    public @Nullable String getShader(Identifier id, ShaderType type) {
                        return shaderSources.get(Pair.of(id, type));
                    }

                    @Override
                    public @Nullable CachedIncludeSource getInclude(Identifier id) {
                        return includeSourcesBuilder.get(id);
                    }

                    @Override
                    public void close() {
                    }

                },
                Runnable::run  // TODO?
            ).join().finishCompile();
        });
    }

}