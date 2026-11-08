package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.light.Lights;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixin.GameRendererAccessor;
import fewizz.canpipe.mixin.GuiRendererAccessor;
import fewizz.canpipe.mixin.LevelRendererAccessor;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.load.PipelineLodader;
import fewizz.canpipe.pipeline.load.RawPipelineLoader;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Pipelines implements PreparableReloadListener {

    public static final Pipelines INSTANCE = new Pipelines();
    private Pipelines() {}

    public static final Map<Identifier, RawPipeline> RAW_PIPELINES = new LinkedHashMap<>();

    private static @Nullable RawPipeline currentRaw = null;
    private static @Nullable Throwable loadingError = null;
    private static @Nullable Pipeline current = null;

    public static @Nullable Pipeline getCurrent() { return current;}
    public static @Nullable RawPipeline getCurrentRaw() { return currentRaw; }
    public static @Nullable Throwable getLoadingError() { return loadingError; }

    public static void loadAndApply(@Nullable RawPipeline raw, Pair<OptionGroup.Element<?>, @Nullable Object> optionsChanges) {
        loadAndApply(raw, optionsChanges, true /* save selected pipeline, i.e. it will be selected on next game reload */);
    }

    public static void loadAndApply(@Nullable RawPipeline raw, Pair<OptionGroup.Element<?>, @Nullable Object> optionsChanges, boolean saveSelectedPipeline) {
        assert RenderSystem.isOnRenderThread();

        Pipelines.loadingError = null;
        Pipelines.currentRaw = raw;

        // delete previous compilation errors
        CanPipe.deleteGlslToSpvCompilationErrorLogs();

        Map<OptionGroup.Element<?>, Object> appliedOptions = new HashMap<>();
        JsonObject config = new JsonObject();

        // read config
        if (Files.exists(CanPipe.getConfigurationFilePath())) {
            try {
                config = Jankson.builder().build().load(Files.newInputStream(CanPipe.getConfigurationFilePath()));
            } catch (IOException | SyntaxError e) {
                CanPipe.LOGGER.error("Couldn't load configuration file \""+CanPipe.getConfigurationFilePath()+"\"", e);
            }
        }

        if (raw != null) {
            JsonObject pipelinesOptions = JanksonUtils.objectOrEmpty(config, "pipelinesOptions");
            JsonObject pipelineOptions = JanksonUtils.objectOrEmpty(pipelinesOptions, raw.location.toString());

            for (var e : pipelineOptions.entrySet()) {
                String optionElementName = e.getKey();
                Object optionValue = ((JsonPrimitive) e.getValue()).getValue();

                OptionGroup.Element<?> optionElement = null;
                for (var option : raw.options.values()) {
                    optionElement = option.elements().get(optionElementName);
                    if (optionElement != null) {
                        appliedOptions.put(optionElement, optionValue);
                        break;
                    }
                }
            }

            if (optionsChanges != null) {
                if (optionsChanges.getValue() != null) {
                    appliedOptions.put(optionsChanges.getKey(), optionsChanges.getValue());
                }
                else {
                    appliedOptions.remove(optionsChanges.getKey());
                }
            }
        }

        // save config
        if (saveSelectedPipeline) {
            config.put("current", raw != null ? new JsonPrimitive(raw.location.toString()) : JsonNull.INSTANCE);
        }

        if (raw != null) {
            var pipelineOptions = new JsonObject();

            for (var kv : appliedOptions.entrySet()) {
                pipelineOptions.put(kv.getKey().name, new JsonPrimitive(kv.getValue()));
            }

            JsonObject pipelinesOptions = (JsonObject) config.computeIfAbsent("pipelinesOptions", k -> new JsonObject());
            pipelinesOptions.put(raw.location.toString(), pipelineOptions);
        }

        try {
            Files.createDirectories(CanPipe.getConfigurationFilePath().getParent());
            Files.writeString(CanPipe.getConfigurationFilePath(), config.toJson(true, true));
        } catch (IOException e) {
            CanPipe.LOGGER.error("Couldn't save configuration file \""+CanPipe.getConfigurationFilePath()+"\"", e);
        }

        // "load" part
        Pipeline loadedPipeline = null;

        // Flushes main command buffer
        // Main command buffer could already be created at this point, for example because of texture loading
        RenderSystem.getDevice().createCommandEncoder().submit();

        if (raw != null) {
            try {
                loadedPipeline = PipelineLodader.load(raw, appliedOptions);
            } catch (Throwable e) {
                CanPipe.LOGGER.error("Couldn't load pipeline \""+raw.location+"\"", e);
                Pipelines.loadingError = e;
            }
        }

        Pipelines.apply(loadedPipeline);
    }

    public static void apply(Pipeline loadedPipeline) {
        Minecraft mc = Minecraft.getInstance();

        ((GameRendererExtended) mc.gameRenderer).canpipe_setMainRenderTargetOverride(
            loadedPipeline != null ? loadedPipeline.targets.main() : null
        );

        if (Pipelines.current != null) {
            Pipelines.current.close();
        }

        Pipeline prevPipeline = Pipelines.current;
        Pipelines.current = loadedPipeline;

        if (Pipelines.current != null) {
            ((GameRendererExtended) mc.gameRenderer).canpipe_onPipelineActivated();
        }

        boolean prevPipelineUnloaded = prevPipeline != null;
        boolean newPipelineLoaded = loadedPipeline != null;
        if (prevPipelineUnloaded != newPipelineLoaded) {
            var srd = mc.levelRenderer.sectionRenderDispatcher();
            if (srd != null) { srd.dispose(); }
            ((LevelRendererAccessor) mc.levelRenderer).canpipe_setSectionRenderDispatcher(null);
            mc.levelExtractor.allChanged();
            mc.levelExtractor.resetSampler();
        }

        // LevelExtractor.shouldResetSkyRenderer may be ignored if level has no sky
        SkyRenderer skyRenderer = ((LevelRendererAccessor) mc.levelRenderer).canpipe_getSkyRenderer();
        if (skyRenderer != null) {
            skyRenderer.close();
            ((LevelRendererAccessor) mc.levelRenderer).canpipe_setSkyRenderer(null);
        }

        var guiRenderer = ((GameRendererAccessor) mc.gameRenderer).canpipe_getGuiRenderer();
        ((GuiRendererAccessor) guiRenderer).canpipe_invalidateItemAtlas();
    }

    public static void reloadCurrent() {
        Minecraft mc = Minecraft.getInstance();
        var resourceManager = mc.getResourceManager();
        Lights.loadRaw(Lights.readRaw(resourceManager));
        Materials.loadRaw(Materials.readRaw(resourceManager));
        MaterialMaps.loadRaw(MaterialMaps.readRaw(resourceManager));
        Pipelines.loadRaw(Pipelines.readRaw(resourceManager));
    }

    private static Map<Identifier, RawPipeline> readRaw(ResourceManager resourceManager) {
        Map<Identifier, RawPipeline> rawPipelines = new LinkedHashMap<>();
        resourceManager.listResources(
            "pipelines",
            (Identifier pipelineLocation) -> {
                String pathStr = pipelineLocation.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((location, pipelineJson) -> {
            try {
                JsonObject json = Jankson.builder().build().load(pipelineJson.open());
                rawPipelines.put(location, RawPipelineLoader.load(json, location, resourceManager));
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't parse pipeline json file \""+location+"\"", e);
            }
        });
        return rawPipelines;
    }

    private static void loadRaw(Map<Identifier, RawPipeline> rawPipelines) {
        RAW_PIPELINES.clear();
        RAW_PIPELINES.putAll(rawPipelines);
        RawPipeline selected = null;
        if (Files.exists(CanPipe.getConfigurationFilePath())) {
            try {
                JsonObject readOptions = Jankson.builder().build().load(
                    Files.newInputStream(CanPipe.getConfigurationFilePath())
                );
                String currentLocationStr = readOptions.get(String.class, "current");
                if (currentLocationStr != null) {
                    selected = RAW_PIPELINES.get(Identifier.parse(currentLocationStr));
                }
            } catch (IOException | SyntaxError e) {
                CanPipe.LOGGER.error("Couldn't load configuration file \""+CanPipe.getConfigurationFilePath()+"\"", e);
            }
        }

        loadAndApply(selected, null);
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(
        PreparableReloadListener.@NonNull SharedState sharedState,
        @NonNull Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        @NonNull Executor applyExecutor
    ) {
        return CompletableFuture
            .supplyAsync(() -> Pipelines.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(Pipelines::loadRaw, applyExecutor);
    }

}
