package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

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
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class Pipelines implements PreparableReloadListener {
    static final Path CONFIG_PATH = Path.of("config/can-pipe.json");
    public static final Map<ResourceLocation, PipelineRaw> RAW_PIPELINES = new LinkedHashMap<>();

    private static volatile Pipeline current = null;

    public static void loadAndSetPipeline(
        PipelineRaw raw,
        @Nullable Map<Option.Element<?>, Object> optionsChanges
    ) throws Exception {
        assert RenderSystem.isOnRenderThread();

        Pipeline pipeline = null;
        Map<Option.Element<?>, Object> appliedOptions = optionsChanges != null ? new HashMap<>(optionsChanges) : new HashMap<>();

        JsonObject config = new JsonObject();

        if (Files.exists(CONFIG_PATH)) {
            config = CanPipe.JANKSON.load(Files.newInputStream(CONFIG_PATH));
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

            if (optionsChanges != null) {
                appliedOptions.putAll(optionsChanges);
            }

            pipeline = new Pipeline(raw, appliedOptions);
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

                Function<Option.Element<?>, String> optionName = (Option.Element<?> e) -> {
                    for (var option : raw.options.values()) {
                        for (var entry : option.elements.entrySet()) {
                            String elementName = entry.getKey();
                            Option.Element<?> element = entry.getValue();
                            if (element == e) {
                                return elementName;
                            }
                        }
                    }
                    throw new RuntimeException("Couldn't file option element "+e);
                };

                var pipelineOptions = new JsonObject();
                for (var kv : appliedOptions.entrySet()) {
                    pipelineOptions.put(optionName.apply(kv.getKey()), new JsonPrimitive(kv.getValue()));
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
                return resourceManager.listResources(
                    "pipelines",
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, Resource> jsons) -> {
                RAW_PIPELINES.clear();

                jsons.forEach((loc, pipelineRawJson) -> {
                    try {
                        JsonObject o = CanPipe.JANKSON.load(pipelineRawJson.open());

                        Map<String, JsonObject> includes = new HashMap<>();
                        processIncludes(o, includes, resourceManager);

                        Map<ResourceLocation, Option> options = new LinkedHashMap<>();

                        for (var optionGroups : JanksonUtils.listOfObjects(o, "options")) {
                            ResourceLocation includeToken = ResourceLocation.parse(optionGroups.get(String.class, "includeToken"));
                            var elementsO = optionGroups.getObject("elements");
                            if (elementsO == null) {  // compat
                                elementsO = optionGroups.getObject("options");
                            }

                            var categoryKey = optionGroups.get(String.class, "categoryKey");

                            if (elementsO != null) {
                                Map<String, Option.Element<?>> elements = new LinkedHashMap<>();
                                for (var elementE : elementsO.entrySet()) {
                                    String name = elementE.getKey();
                                    JsonObject elementO = (JsonObject) elementE.getValue();

                                    var defaultValue = elementO.get(JsonPrimitive.class, "default").getValue();
                                    String nameKey = elementO.get(String.class, "nameKey");

                                    var prefix = elementO.get(String.class, "prefix");
                                    var choices = JanksonUtils.listOfStrings(elementO, "choices");
                                    choices = choices.size() == 0 ? null : choices;

                                    Option.Element<?> element;
                                    if (choices != null) {
                                        element = new Option.EnumElement((String) defaultValue, nameKey, prefix, choices);
                                    }
                                    else if (defaultValue instanceof Number) {
                                        var min = (Number) elementO.get(JsonPrimitive.class, "min").getValue();
                                        var max = (Number) elementO.get(JsonPrimitive.class, "max").getValue();
                                        if (defaultValue instanceof Double) {
                                            element = new Option.FloatElement((double) defaultValue, nameKey, (double) min, (double) max);
                                        }
                                        else if (defaultValue instanceof Long) {
                                            element = new Option.IntegerElement((long) defaultValue, nameKey, (long) min, (long) max);
                                        }
                                        else {
                                            throw new NotImplementedException();
                                        }
                                    }
                                    else if (defaultValue instanceof Boolean) {
                                        element = new Option.BooleanElement((boolean) defaultValue, nameKey);
                                    }
                                    else {
                                        throw new NotImplementedException();
                                    }

                                    elements.put(name, element);
                                }
                                options.put(includeToken, new Option(includeToken, categoryKey, elements));
                            }
                        }
                        o.remove("options");

                        String nameKey = o.get(String.class, "nameKey");

                        RAW_PIPELINES.put(loc, new PipelineRaw(loc, nameKey, options, o));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                PipelineRaw pipelineRaw = null;

                if (Files.exists(CONFIG_PATH)) {
                    try {
                        JsonObject readOptions = CanPipe.JANKSON.load(Files.newInputStream(CONFIG_PATH));
                        String current = readOptions.get(String.class, "current");
                        if (current != null) {
                            pipelineRaw = RAW_PIPELINES.get(ResourceLocation.parse(current));
                        }
                    } catch (IOException | SyntaxError e) {
                        e.printStackTrace();
                    }
                }

                try {
                    loadAndSetPipeline(pipelineRaw, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
