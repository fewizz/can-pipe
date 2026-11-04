package fewizz.canpipe.pipeline.load;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramProvider;
import fewizz.canpipe.pipeline.OptionGroup;
import fewizz.canpipe.pipeline.Pass;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.RawPipeline;
import fewizz.canpipe.pipeline.Texture;
import fewizz.canpipe.renderpearl.GpuDeviceExtended;
import fewizz.canpipe.renderpearl.GpuTextureViewExtended;
import fewizz.canpipe.util.JanksonUtils;
import fewizz.canpipe.util.MoonPhasesTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public class PipelineLodader {

    private static final RenderPipeline[] RENDER_PIPELINES_TO_REPLACE = new RenderPipeline[] {
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


    public static Pipeline load(RawPipeline rawPipeline, Map<OptionGroup.Element<?>, Object> appliedOptions) throws Throwable {
        JsonObject pipelineJson = rawPipeline.getPipelineJson(appliedOptions);
        PipelinesFixes.fix(rawPipeline.location, pipelineJson);

        Pipeline.Settings settings = new Pipeline.Settings(
            pipelineJson.getBoolean("enablePBR", false),
            pipelineJson.getInt("glslVersion", 330),
            (float) Math.toRadians(JanksonUtils.objectOrEmpty(pipelineJson, "sky").getFloat("defaultZenithAngle", 0.0F)),
            pipelineJson.getBoolean("smoothBrightnessBidirectionaly", false),
            pipelineJson.getInt("brightnessSmoothingFrames", 20),
            pipelineJson.getInt("rainSmoothingFrames", 500),
            pipelineJson.getInt("thunderSmoothingFrames", 500),
            rawPipeline.awareOfDepthRangeChanges
        );

        Map<String, Texture> textures = new HashMap<>();
        Map<String, Framebuffer> framebuffers = new HashMap<>();
        Map<RenderPipeline, MaterialProgramProvider> materialProgramProviders = new HashMap<>();
        Map<String, AbstractTexture> materialProgramSamplerTextures = new HashMap<>();
        Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, CompiledRenderPipeline> programs = new HashMap<>();

        Pipeline.Passes passes = new Pipeline.Passes(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        Pipeline.Shadows shadows = null;
        Pipeline.Targets targets = null;
        Pipeline.FabulousTargets fabulousTargets = null;

        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
        Executor localExecutor = runnable -> { tasks.add(runnable); };

        try {

        // "images"
        Function<String, Optional<Texture>> getOrLoadOptionalTexture = (String name) -> {
            return Optional.ofNullable(textures.computeIfAbsent(name, _name -> {
                List<JsonObject> texturesJsons = JanksonUtils.listOfObjects(pipelineJson, "images");
                Optional<JsonObject> possibleJson = texturesJsons.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                if (possibleJson.isEmpty()) {
                    return null;
                }
                try {
                    return TextureLoader.load(possibleJson.get());
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
                    Minecraft mc = Minecraft.getInstance();
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
            return Optional.ofNullable(framebuffers.computeIfAbsent(name, _name -> {
                try {
                    List<JsonObject> framebuffersJsons = JanksonUtils.listOfObjects(pipelineJson, "framebuffers");
                    Optional<JsonObject> possibleJson = framebuffersJsons.stream().filter(t -> JanksonUtils.stringOrThrow(t, "name").equals(name)).findFirst();
                    if (possibleJson.isEmpty()) {
                        return null;
                    }
                    return FramebufferLoader.load(possibleJson.get(), rawPipeline.location, getOrLoadTexture);
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

        targets = new Pipeline.Targets(
            defaultFramebuffer,
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "solidTerrain")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentTerrain")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentEntity")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "translucentParticles")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "weather")),
            getOrLoadFramebuffer.apply(targetsJson.get(String.class, "clouds"))
        );

        JsonObject fabulousTargetsJson = pipelineJson.getObject("fabulousTargets");
        fabulousTargets = fabulousTargetsJson == null ? null : new Pipeline.FabulousTargets(
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "translucent")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "entity")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "particles")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "weather")),
            getOrLoadFramebuffer.apply(fabulousTargetsJson.get(String.class, "clouds"))
        );
        if (pipelineJson.containsKey("fabulous") && fabulousTargets == null) {
            throw new RuntimeException("To use \"fabulous\" passes, \"fabulousTargets\" should be defined");
        }

        Map<Identifier, String> shaderSourceCache = new HashMap<>();

        Function<Identifier, Optional<String>> getShaderSource = (Identifier shaderLocation) -> {
            RenderSystem.assertOnRenderThread();

            String source = shaderSourceCache.computeIfAbsent(shaderLocation, (loc) -> {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    var resource = mc.getResourceManager().getResource(shaderLocation);
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

        CompletableFuture<Void> materialProgramLoadersAreReady = CompletableFuture.completedFuture(null);

        JsonObject shadowsJson = pipelineJson.getObject("skyShadows");
        if (shadowsJson != null) {
            var shadowsAndFutureMaterialProgram = loadShadowsAndMaterialProgram(
                rawPipeline, pipelineJson, shadowsJson, appliedOptions,
                getShaderSource, getOrLoadFramebuffer,
                framebuffers, settings, localExecutor
            );
            shadows = shadowsAndFutureMaterialProgram.getLeft();
            materialProgramLoadersAreReady = CompletableFuture.allOf(materialProgramLoadersAreReady, shadowsAndFutureMaterialProgram.getRight());
        }

        var finalShadows = shadows;

        // "materialProgram"
        materialProgramLoadersAreReady = loadMaterialProgram(
            rawPipeline, pipelineJson, appliedOptions,
            getShaderSource, getOrLoadFramebuffer, getOrLoadPipelineOrResourcepackTexture, materialProgramSamplerTextures,
            materialProgramProviders, settings, shadows, localExecutor
        );

        // "programs"

        Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, CompletableFuture<CompiledRenderPipeline>> futurePrograms = new HashMap<>();

        BiFunction<String, Framebuffer, CompletableFuture<CompiledRenderPipeline>> getOrLoadProgram = (String name, Framebuffer fb) -> {
            RenderSystem.assertOnRenderThread();

            return futurePrograms.computeIfAbsent(Pair.of(name, fb.getFormats()), _name -> {
                List<JsonObject> programsJsons = JanksonUtils.listOfObjects(pipelineJson, "programs");
                Optional<JsonObject> programJson = programsJsons.stream().filter(program -> JanksonUtils.stringOrThrow(program, "name").equals(name)).findFirst();
                if (programJson.isEmpty()) {
                    throw new RuntimeException("Couldn't find program \""+name+"\"");
                }
                try {
                    return ProgramLoader.load(
                        programJson.get(), rawPipeline.location, getShaderSource, settings.glslVersion(),
                        rawPipeline.options, appliedOptions, Optional.ofNullable(finalShadows != null ? finalShadows.shadowMapSize() : null),
                        fb, settings.awareOfDepthRangeChanges(),
                        localExecutor
                    ).thenApplyAsync(
                        compiledRenderPipeline -> {
                            if (compiledRenderPipeline == null) {
                                throw new RuntimeException("Couldn't compile program \""+name+"\"");
                            }
                            return compiledRenderPipeline;
                        },
                        localExecutor
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't load program \""+name+"\"", e);
                }
            });
        };

        CompletableFuture<Void> passesAreLoaded = loadPasses(
            rawPipeline, pipelineJson, appliedOptions,
            getOrLoadOptionalFramebuffer, getOrLoadPipelineOrResourcepackTexture, getOrLoadProgram,
            passes, localExecutor
        );

        List<Throwable> throwables = new ArrayList<>();

        while (!passesAreLoaded.isDone() || !materialProgramLoadersAreReady.isDone() || !tasks.isEmpty()) {
            try {
                tasks.take().run();
            } catch (Throwable t) {
                CanPipe.LOGGER.error("Exception occured while compiling render pipelines", t);
                throwables.add(t);
            }
        }

        if (materialProgramLoadersAreReady.isCompletedExceptionally()) {
            throwables.add(materialProgramLoadersAreReady.exceptionNow());
        }

        futurePrograms.forEach((formats, futureCompiledProgram) -> {
            if (futureCompiledProgram.isCompletedExceptionally()) {
                throwables.add(futureCompiledProgram.exceptionNow());
            }
            else {
                programs.put(formats, futureCompiledProgram.join());
            }
        });

        if (!throwables.isEmpty()) {
            throw throwables.get(0);
        }

        } catch (Throwable t) {
            passes.close();

            framebuffers.values().forEach(Framebuffer::destroyBuffers);
            textures.values().forEach(Texture::close);

            programs.values().stream().forEach(CompiledRenderPipeline::close);

            materialProgramProviders.values().forEach(MaterialProgramProvider::close);

            if (shadows != null) {
                shadows.materialProgramProviders().values().forEach(MaterialProgramProvider::close);
            }

            throw t;
        }

        return new Pipeline(
            rawPipeline.location, appliedOptions,
            settings, passes, shadows, targets, fabulousTargets,
            textures, framebuffers, materialProgramProviders, materialProgramSamplerTextures, programs
        );
    }

    static CompletableFuture<Void> loadMaterialProgram(
        RawPipeline rawPipeline,
        JsonObject pipelineJson,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<Identifier, Optional<String>> getShaderSource,
        Function<String, Framebuffer> getOrLoadFramebuffer,
        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture,
        Map<String, AbstractTexture> materialProgramSamplerTextures,
        Map<RenderPipeline, MaterialProgramProvider> materialProgramProviders,
        Pipeline.Settings settings,
        Pipeline.Shadows shadows,
        Executor localExecutor
    ) {
        JsonObject materialProgramJson = JanksonUtils.objectOrThrow(pipelineJson, "materialProgram");

        List<String> samplers = new ArrayList<>(JanksonUtils.listOfStrings(materialProgramJson, "samplers"));
        if (shadows != null) {
            samplers.add("frxs_shadowMap");
            samplers.add("frxs_shadowMapTexture");
        }

        var materialVertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgramJson, "vertexSource"));
        var materialFragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(materialProgramJson, "fragmentSource"));

        CompletableFuture<Void> materialProgramLoadersAreReady = CompletableFuture.allOf(
            Stream.of(RENDER_PIPELINES_TO_REPLACE)
            .map(
                renderPipeline -> MaterialProgramLoader.load(
                    renderPipeline, settings, false, shadows,
                    materialVertexShaderLocation, materialFragmentShaderLocation,
                    rawPipeline.options, appliedOptions, samplers, getShaderSource
                ).whenCompleteAsync(
                    (materialProgramLoader, throwable) -> {
                        if (throwable != null) { throw new RuntimeException(throwable); }
                        materialProgramProviders.put(materialProgramLoader.originalRenderPipeline(), materialProgramLoader);
                    },
                    localExecutor
                )
            ).toArray(CompletableFuture[]::new)
        );

        var samplerImagesNames = JanksonUtils.listOfStrings(materialProgramJson, "samplerImages");
        for (int i = 0; i < Math.min(samplers.size(), samplerImagesNames.size()); ++i) {
            String sampler = samplers.get(i);
            var samplerImage = getOrLoadPipelineOrResourcepackTexture.apply(samplerImagesNames.get(i));
            if (samplerImage.isEmpty()) {
                throw new RuntimeException("Couldn't find material program sampler image \""+samplerImagesNames.get(i)+"\"");
            }
            materialProgramSamplerTextures.put(sampler, samplerImage.get());
        }
        if (shadows != null) {
            String shadowMapTextureName = shadows.framebuffers().get(0).getDepthTexture().getLabel();
            Optional<AbstractTexture> shadowMapTexture = getOrLoadPipelineOrResourcepackTexture.apply(shadowMapTextureName);
            if (shadowMapTexture.isEmpty()) {
                throw new RuntimeException("Couldn't find material program shadowmap image \""+shadowMapTextureName+"\"");
            }
            materialProgramSamplerTextures.put("frxs_shadowMap", shadowMapTexture.get());
            materialProgramSamplerTextures.put("frxs_shadowMapTexture", shadowMapTexture.get());
        }

        return materialProgramLoadersAreReady;
    }

    static Pair<Pipeline.Shadows, CompletableFuture<Void>> loadShadowsAndMaterialProgram(
        RawPipeline rawPipeline,
        JsonObject pipelineJson,
        JsonObject shadowsJson,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<Identifier, Optional<String>> getShaderSource,
        Function<String, Framebuffer> getOrLoadFramebuffer,
        Map<String, Framebuffer> framebuffers,
        Pipeline.Settings settings,
        Executor localExecutor
    ) {
        Framebuffer shadowFramebuffer = getOrLoadFramebuffer.apply(shadowsJson.get(String.class, "framebuffer"));
        Objects.requireNonNull(shadowFramebuffer.getDepthTexture());
        Objects.requireNonNull(shadowFramebuffer.getDepthTextureView());

        // Instead of one shadow framebuffer, we create N (= number of cascades) framebuffers for different layers
        List<Framebuffer> shadowFramebuffers = new ArrayList<>();
        int baseArrayLayer = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer();
        int layerCount = ((GpuTextureViewExtended) shadowFramebuffer.getDepthTextureView()).canpipe_layerCount();
        var cascadeRadii = JanksonUtils.listOfIntegers(shadowsJson, "cascadeRadius");
        for (int i = 0; i < cascadeRadii.size() + 1; ++i) {
            final int cascade = i;
            GpuTexture shadowMapTexture = shadowFramebuffer.getDepthTexture();
            GpuTextureView shadowMapTextureView = shadowFramebuffer.getDepthTextureView();
            Framebuffer fb = new Framebuffer(
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
            shadowFramebuffers.add(fb);
            framebuffers.put(fb.name, fb);
        }

        var vertexShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "vertexSource"));
        var fragmentShaderLocation = Identifier.parse(JanksonUtils.stringOrThrow(shadowsJson, "fragmentSource"));

        Pipeline.Shadows shadows = new Pipeline.Shadows(
            new HashMap<>(),
            shadowFramebuffers,
            cascadeRadii,
            shadowsJson.getFloat("offsetSlopeFactor", 1.1F),
            shadowsJson.getFloat("offsetBiasUnits", 4.0F),
            shadowsJson.getBoolean("supportForwardRender", true),
            shadowsJson.getBoolean("allowEntities", true),
            shadowsJson.getBoolean("allowParticles", true)
        );

        return Pair.of(shadows, CompletableFuture.allOf(
            Stream.of(RENDER_PIPELINES_TO_REPLACE)
            .map(
                renderPipeline -> MaterialProgramLoader.load(
                    renderPipeline, settings, true, shadows,
                    vertexShaderLocation, fragmentShaderLocation, rawPipeline.options, appliedOptions,
                    List.of(), getShaderSource
                ).whenCompleteAsync(
                    (materialProgramProvider, throwable) -> {
                        if (materialProgramProvider != null) {
                            shadows.materialProgramProviders().put(materialProgramProvider.originalRenderPipeline(), materialProgramProvider);
                        }
                    },
                    localExecutor
                )
            ).toArray(CompletableFuture[]::new)
        ));
    }

    static CompletableFuture<Void> loadPasses(
        RawPipeline rawPipeline,
        JsonObject pipelineJson,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<String, Optional<Framebuffer>> getOrLoadOptionalFramebuffer,
        Function<String, Optional<AbstractTexture>> getOrLoadPipelineOrResourcepackTexture,
        BiFunction<String, Framebuffer, CompletableFuture<CompiledRenderPipeline>> getOrLoadProgram,
        Pipeline.Passes passes,
        Executor localExecutor
    ) {
        // passes

        Function<String, Object> optionValueByName = (String name) -> {
            var element = rawPipeline.optionElementByName(name);
            if (element == null) {
                return null;
            }
            return appliedOptions.getOrDefault(element, element.defaultValue);
        };

        BiFunction<String, List<Pass>, CompletableFuture<Void>> loadPasses = (name, passesList) -> {
            JsonObject passesJson = pipelineJson.getObject(name);
            if (passesJson == null) { return CompletableFuture.completedFuture(null); }

            List<CompletableFuture<Optional<Pass>>> futures = new ArrayList<>();

            for (var passJson : JanksonUtils.listOfObjects(passesJson, "passes")) {
                var future = PassLoader.load(
                    rawPipeline.location,
                    passJson, optionValueByName,
                    getOrLoadOptionalFramebuffer,
                    getOrLoadProgram,
                    getOrLoadPipelineOrResourcepackTexture,
                    localExecutor
                );
                futures.add(future);
            }

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenCompleteAsync(
                (_, _) -> {
                    RenderSystem.assertOnRenderThread();
                    passesList.addAll(
                        futures.stream()
                        .filter(f -> !f.isCompletedExceptionally() && f.join().isPresent())
                        .map(f -> f.join().get())
                        .collect(Collectors.toList()));
                },
                localExecutor
            );
        };

        return CompletableFuture.allOf(
            loadPasses.apply("onInit", passes.onInit()),
            loadPasses.apply("onResize", passes.onResize()),
            loadPasses.apply("beforeWorldRender", passes.beforeWorldRender()),
            loadPasses.apply("fabulous", passes.fabulous()),
            loadPasses.apply("afterRenderHand", passes.afterRenderHand())
        );
    }

}
