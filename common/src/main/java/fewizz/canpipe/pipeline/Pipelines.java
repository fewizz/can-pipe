package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonNull;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class Pipelines implements PreparableReloadListener {
    static final Path CONFIG_PATH = Path.of("config/can-pipe.json");
    public static final Map<ResourceLocation, PipelineRaw> RAW_PIPELINES = new LinkedHashMap<>();

    private static volatile Pipeline current = null;

    public static boolean loadAndSetPipeline(
        @Nullable PipelineRaw raw,
        @Nullable Map<Option.Element<?>, Object> optionsChanges
    ) {
        assert RenderSystem.isOnRenderThread();

        Pipeline pipeline = null;
        Map<Option.Element<?>, Object> appliedOptions = new HashMap<>();

        JsonObject config = new JsonObject();

        if (Files.exists(CONFIG_PATH)) {
            try {
                config = CanPipe.JANKSON.load(Files.newInputStream(CONFIG_PATH));
            } catch (IOException | SyntaxError e) {
                e.printStackTrace();
            }
        }

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
                e.printStackTrace();
                return false;
            }
        }

        Minecraft mc = Minecraft.getInstance();

        if (current != null) {
            current.close();
            if (mc.mainRenderTarget != null) {
                mc.mainRenderTarget.destroyBuffers();
            }
            mc.mainRenderTarget = null;
        }

        current = pipeline;

        try {
            if (current != null) {
                config.put("current", new JsonPrimitive(current.location.toString()));

                var pipelineOptions = new JsonObject();
                for (var kv : appliedOptions.entrySet()) {
                    pipelineOptions.put(kv.getKey().name, new JsonPrimitive(kv.getValue()));
                }

                JsonObject pipelinesOptions = (JsonObject) config.computeIfAbsent("pipelinesOptions", (k) -> new JsonObject());
                pipelinesOptions.put(current.location.toString(), pipelineOptions);
            }
            else {
                config.put("current", JsonNull.INSTANCE);
            }
            Files.writeString(CONFIG_PATH, config.toJson(true, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (pipeline != null) {
            mc.mainRenderTarget = pipeline.defaultFramebuffer;
            ((GameRendererAccessor) mc.gameRenderer).canpipe_onPipelineActivated();
        }
        else {
            // Pipeline wasn't selected and main render target was destroyed previously
            if (mc.mainRenderTarget == null) {
                mc.mainRenderTarget = new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            }
        }

        mc.levelRenderer.allChanged();

        return true;
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
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                ).forEach((location, pipelineJson) -> {
                    JsonObject o;
                    try {
                        o = CanPipe.JANKSON.load(pipelineJson.open());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    Map<String, JsonObject> includes = new HashMap<>();
                    try {
                        processIncludes(o, includes, resourceManager);
                    } catch (IOException | SyntaxError e) {
                        e.printStackTrace();
                        return;
                    }

                    Map<ResourceLocation, Option> options = new LinkedHashMap<>();

                    for (var optionsA : JanksonUtils.listOfObjects(o, "options")) {
                        ResourceLocation includeToken = ResourceLocation.parse(optionsA.get(String.class, "includeToken"));
                        var elementsO = optionsA.getObject("elements");
                        if (elementsO == null) {  // compat
                            elementsO = optionsA.getObject("options");
                        }
                        if (elementsO == null) {
                            elementsO = new JsonObject();
                        }

                        var categoryKey = optionsA.get(String.class, "categoryKey");

                        Map<String, Option.Element<?>> elements = new LinkedHashMap<>();
                        for (var entry : elementsO.entrySet()) {
                            String name = entry.getKey();
                            JsonObject elementO = (JsonObject) entry.getValue();

                            var defaultValue = elementO.get(JsonPrimitive.class, "default").getValue();
                            String nameKey = elementO.get(String.class, "nameKey");

                            var prefix = elementO.get(String.class, "prefix");
                            var choices = JanksonUtils.listOfStrings(elementO, "choices");
                            choices = choices.size() == 0 ? null : choices;

                            Option.Element<?> element;
                            if (choices != null) {
                                element = new Option.EnumElement(name, (String) defaultValue, nameKey, prefix, choices);
                            }
                            else if (defaultValue instanceof Number) {
                                var min = (Number) elementO.get(JsonPrimitive.class, "min").getValue();
                                var max = (Number) elementO.get(JsonPrimitive.class, "max").getValue();
                                if (defaultValue instanceof Double) {
                                    element = new Option.FloatElement(name, (double) defaultValue, nameKey, (double) min, (double) max);
                                }
                                else if (defaultValue instanceof Long) {
                                    element = new Option.IntegerElement(name, (long) defaultValue, nameKey, (long) min, (long) max);
                                }
                                else {
                                    throw new NotImplementedException();
                                }
                            }
                            else if (defaultValue instanceof Boolean) {
                                element = new Option.BooleanElement(name, (boolean) defaultValue, nameKey);
                            }
                            else {
                                throw new NotImplementedException();
                            }

                            elements.put(name, element);
                        }
                        options.put(includeToken, new Option(includeToken, categoryKey, elements));
                    }

                    o.remove("options");

                    String nameKey = o.get(String.class, "nameKey");

                    rawPipelines.put(location, new PipelineRaw(location, nameKey, options, o));
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
                        String current = readOptions.get(String.class, "current");
                        if (current != null) {
                            selected = RAW_PIPELINES.get(ResourceLocation.parse(current));
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

    private static void processIncludes(
        JsonObject object,
        Map<String, JsonObject> includes,
        ResourceManager manager
    ) throws IOException, SyntaxError {
        JsonArray includesArray = (JsonArray) object.remove("include");
        if (includesArray == null) {
            return;
        }
        for (var path : includesArray) {
            JsonObject toInclude = includes.getOrDefault(path, null);
            if (toInclude == null) {
                String pathStr = ((JsonPrimitive) path).asString();
                toInclude = CanPipe.JANKSON.load(manager.open(ResourceLocation.parse(pathStr)));
                processIncludes(toInclude, includes, manager);
                includes.put(pathStr, toInclude);
            }
            JanksonUtils.mergeJsonObjectB2A(object, toInclude);
        }
    }

    public static @Nullable Pipeline getCurrent() {
        return current;
    }

}
