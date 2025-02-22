package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class Pipelines implements PreparableReloadListener {
    static final Path CONFIG_PATH = Path.of("config/can-pipe.json");
    public static final Map<ResourceLocation, PipelineRaw> RAW_PIPELINES = new LinkedHashMap<>();

    private static volatile @Nullable PipelineRaw currentRaw = null;
    private static volatile @Nullable Throwable loadingError = null;
    private static volatile @Nullable Pipeline current = null;

    public static void loadAndSetPipeline(
        @Nullable PipelineRaw raw,
        Map<Option.Element<?>, Object> optionsChanges
    ) {
        assert RenderSystem.isOnRenderThread();

        Pipelines.loadingError = null;
        Pipelines.currentRaw = null;

        Pipeline pipeline = null;
        Map<Option.Element<?>, Object> appliedOptions = new HashMap<>();

        JsonObject config = new JsonObject();

        // read config
        if (Files.exists(CONFIG_PATH)) {
            try {
                config = CanPipe.JANKSON.load(Files.newInputStream(CONFIG_PATH));
            } catch (IOException | SyntaxError e) {
                e.printStackTrace();
            }
        }

        // save config
        try {
            if (raw != null) {
                config.put("current", new JsonPrimitive(raw.location.toString()));

                var pipelineOptions = new JsonObject();
                for (var kv : appliedOptions.entrySet()) {
                    pipelineOptions.put(kv.getKey().name, new JsonPrimitive(kv.getValue()));
                }

                JsonObject pipelinesOptions = (JsonObject) config.computeIfAbsent("pipelinesOptions", k -> new JsonObject());
                pipelinesOptions.put(raw.location.toString(), pipelineOptions);
            }
            else {
                config.put("current", JsonNull.INSTANCE);
            }
            Files.writeString(CONFIG_PATH, config.toJson(true, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // "load" part
        if (raw != null) {
            JsonObject pipelinesOptions = config.getObject("pipelinesOptions");
            pipelinesOptions = pipelinesOptions == null ? new JsonObject() : pipelinesOptions;

            JsonObject pipelineOptions = pipelinesOptions.getObject(raw.location.toString());
            pipelineOptions = pipelineOptions == null ? new JsonObject() : pipelineOptions;

            for (var entry : pipelineOptions.entrySet()) {
                var optionElementName = entry.getKey();
                var optionValue = ((JsonPrimitive) entry.getValue()).getValue();

                Option.Element<?> optionElement = null;
                for (var option : raw.options.values()) {
                    optionElement = option.elements.get(optionElementName);
                    if (optionElement != null) {
                        appliedOptions.put(optionElement, optionValue);
                        break;
                    }
                }
            }

            appliedOptions.putAll(optionsChanges);

            try {
                pipeline = new Pipeline(raw, appliedOptions);
            } catch (Exception e) {
                Pipelines.loadingError = e;
            }
        }

        // "set" part
        Minecraft mc = Minecraft.getInstance();

        mc.mainRenderTarget.destroyBuffers();
        mc.mainRenderTarget =
            pipeline != null ?
            pipeline.defaultFramebuffer :
            new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());

        boolean recompileRegions = false;

        // from vanilla to pipeline, or vice versa
        if ((Pipelines.current == null) != (pipeline == null)) {
            recompileRegions = true;
        }

        if (Pipelines.current != null) {
            Pipelines.current.close();
        }

        Pipelines.current = pipeline;

        if (Pipelines.current != null) {
            ((GameRendererAccessor) mc.gameRenderer).canpipe_onPipelineActivated();
        }

        if (recompileRegions) {
            mc.levelRenderer.allChanged();
        }
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier preparationBarrier,
        ResourceManager resourceManager,
        Executor loadExecutor,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                Map<ResourceLocation, PipelineRaw> rawPipelines = new LinkedHashMap<>();

                resourceManager.listResources(
                    "pipelines",
                    (ResourceLocation pipelineLocation) -> {
                        String pathStr = pipelineLocation.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                ).forEach((location, pipelineJson) -> {
                    try {
                        JsonObject o = CanPipe.JANKSON.load(pipelineJson.open());
                        rawPipelines.put(location, PipelineRaw.load(o, location, resourceManager));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                return rawPipelines;
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, PipelineRaw> rawPipelines) -> {
                RAW_PIPELINES.clear();
                RAW_PIPELINES.putAll(rawPipelines);

                PipelineRaw selected = null;
                if (Files.exists(CONFIG_PATH)) {
                    try {
                        JsonObject readOptions = CanPipe.JANKSON.load(Files.newInputStream(CONFIG_PATH));
                        String currentLocationStr = readOptions.get(String.class, "current");
                        if (currentLocationStr != null) {
                            selected = RAW_PIPELINES.get(ResourceLocation.parse(currentLocationStr));
                        }
                    } catch (IOException | SyntaxError e) {
                        e.printStackTrace();
                    }
                }

                loadAndSetPipeline(selected, Map.of());
            },
            applyExecutor
        );
    }

    public static @Nullable Pipeline getCurrent() {
        return current;
    }

    public static @Nullable PipelineRaw getCurrentRaw() {
        return currentRaw;
    }

    public static @Nullable Throwable getLoadingError() {
        return loadingError;
    }

}
