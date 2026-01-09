package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

final public class Pipelines implements PreparableReloadListener {

    public static final Pipelines INSTANCE = new Pipelines();
    private Pipelines() {}

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState,
        Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(Pipelines::readRawPipelines, loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(Pipelines::loadRawPipelines, applyExecutor);
    }

    public static final Map<Identifier, PipelineRaw> RAW_PIPELINES = new LinkedHashMap<>();

    private static volatile @Nullable PipelineRaw currentRaw = null;
    private static volatile @Nullable Throwable loadingError = null;
    private static volatile @Nullable Pipeline current = null;

    public static void loadAndSetPipeline(@Nullable PipelineRaw raw, Map<Option.Element<?>, Object> optionsChanges) {
        assert RenderSystem.isOnRenderThread();

        Pipelines.loadingError = null;
        Pipelines.currentRaw = raw;

        // delete previous compilation errors
        if (Files.exists(CanPipe.getCompilationErrorsDirPath())) {
            try {
                Files.walkFileTree(CanPipe.getCompilationErrorsDirPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<Option.Element<?>, Object> appliedOptions = new HashMap<>();
        JsonObject config = new JsonObject();

        // read config
        if (Files.exists(CanPipe.getConfigurationFilePath())) {
            try {
                config = CanPipe.JANKSON.load(Files.newInputStream(CanPipe.getConfigurationFilePath()));
            } catch (IOException | SyntaxError e) {
                e.printStackTrace();
            }
        }

        if (raw != null) {
            JsonObject pipelinesOptions = JanksonUtils.objectOrEmpty(config, "pipelinesOptions");
            JsonObject pipelineOptions = JanksonUtils.objectOrEmpty(pipelinesOptions, raw.location.toString());

            for (var e : pipelineOptions.entrySet()) {
                String optionElementName = e.getKey();
                Object optionValue = ((JsonPrimitive) e.getValue()).getValue();

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
        }

        // save config
        config.put("current", raw != null ? new JsonPrimitive(raw.location.toString()) : JsonNull.INSTANCE);

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
            e.printStackTrace();
        }

        // "load" part
        Pipeline loadedPipeline = null;

        if (raw != null) {
            // Flushes main command buffer for Cinnabar, does nothing for GL backend
            // Main command buffer could already be created at this point, because of texture loading
            RenderSystem.getDevice().createCommandEncoder().createFence().close();

            RenderSystem.getDevice().clearPipelineCache();

            try {
                loadedPipeline = new Pipeline(raw, appliedOptions);
            } catch (Exception e) {
                e.printStackTrace();
                Pipelines.loadingError = e;
                RenderSystem.getDevice().clearPipelineCache();
            }
        }

        // "set" part
        Minecraft mc = Minecraft.getInstance();

        mc.mainRenderTarget.destroyBuffers();

        mc.mainRenderTarget =
            loadedPipeline != null ?
            loadedPipeline.defaultFramebuffer :
            new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());

        Pipeline prevPipeline = Pipelines.current;

        if (Pipelines.current != null) {
            Pipelines.current.close();
        }

        Pipelines.current = loadedPipeline;

        if (Pipelines.current != null) {
            ((GameRendererExtended) mc.gameRenderer).canpipe_onPipelineActivated();
        }

        if ((prevPipeline != null) != (loadedPipeline != null)) {
            mc.levelRenderer.allChanged();
            mc.levelRenderer.resetSampler();
        }
    }

    public static Map<Identifier, PipelineRaw> readRawPipelines() {
        Map<Identifier, PipelineRaw> rawPipelines = new LinkedHashMap<>();
        Minecraft mc = Minecraft.getInstance();
        mc.getResourceManager().listResources(
            "pipelines",
            (Identifier pipelineLocation) -> {
                String pathStr = pipelineLocation.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((location, pipelineJson) -> {
            try {
                JsonObject o = CanPipe.JANKSON.load(pipelineJson.open());
                rawPipelines.put(location, PipelineRaw.load(o, location, mc.getResourceManager()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return rawPipelines;
    }

    public static void loadRawPipelines(Map<Identifier, PipelineRaw> rawPipelines) {
        RAW_PIPELINES.clear();
        RAW_PIPELINES.putAll(rawPipelines);
        PipelineRaw selected = null;
        if (Files.exists(CanPipe.getConfigurationFilePath())) {
            try {
                JsonObject readOptions = CanPipe.JANKSON.load(
                    Files.newInputStream(CanPipe.getConfigurationFilePath())
                );
                String currentLocationStr = readOptions.get(String.class, "current");
                if (currentLocationStr != null) {
                    selected = RAW_PIPELINES.get(Identifier.parse(currentLocationStr));
                }
            } catch (IOException | SyntaxError e) {
                e.printStackTrace();
            }
        }

        loadAndSetPipeline(selected, Map.of());
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
