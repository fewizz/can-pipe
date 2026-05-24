package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class PipelineRaw {
    @NotNull public final Identifier location;
    @NotNull public final String nameKey;
    @NotNull public final Map<Identifier, OptionGroup> options;
    @NotNull private final JsonObject json;

    PipelineRaw(Identifier location, String nameKey, Map<Identifier, OptionGroup> options, JsonObject json) {
        this.location = location;
        this.nameKey = nameKey;
        this.options = Collections.unmodifiableMap(options);
        this.json = json;
    }

    static PipelineRaw load(
        JsonObject pipelineJson,
        Identifier pipelineLocation,
        ResourceManager resourceManager
    ) throws IOException, SyntaxError {
        Map<String, JsonObject> includes = new HashMap<>();

        class ProcessIncludes { private static void doProcess(
            Identifier id,
            JsonObject object,
            Map<String, JsonObject> includes,
            ResourceManager manager
        ) throws IOException, SyntaxError {
            for (var pathToInclude : JanksonUtils.listOfStrings(object, "include")) {
                JsonObject toInclude = includes.getOrDefault(pathToInclude, null);
                if (toInclude == null) {
                    try {
                        Identifier idToInclude = Identifier.parse(pathToInclude);
                        toInclude = Jankson.builder().build().load(manager.open(idToInclude));
                        doProcess(idToInclude, toInclude, includes, manager);
                        includes.put(pathToInclude, toInclude);
                    } catch (Exception e) {
                        throw new RuntimeException("Couldn't include \""+pathToInclude+"\" to \""+id+"\"", e);
                    }
                }
                JanksonUtils.mergeJsonObjectB2A(object, toInclude);
            }
        }};
        ProcessIncludes.doProcess(pipelineLocation, pipelineJson, includes, resourceManager);

        Map<Identifier, OptionGroup> options = new LinkedHashMap<>();

        for (var optionsA : JanksonUtils.listOfObjects(pipelineJson, "options")) {
            Identifier includeToken = Identifier.parse(optionsA.get(String.class, "includeToken"));
            var elementsO = optionsA.getObject("elements");
            if (elementsO == null) {  // compat
                elementsO = optionsA.getObject("options");
            }
            if (elementsO == null) {
                elementsO = new JsonObject();
            }

            var categoryKey = optionsA.get(String.class, "categoryKey");

            Map<String, OptionGroup.Element<?>> elements = new LinkedHashMap<>();
            for (var entry : elementsO.entrySet()) {
                String name = entry.getKey();
                JsonObject elementO = (JsonObject) entry.getValue();

                var defaultValue = elementO.get(JsonPrimitive.class, "default").getValue();
                String nameKey = elementO.get(String.class, "nameKey");
                String descriptionKey = elementO.get(String.class, "descriptionKey");

                var prefix = elementO.get(String.class, "prefix");
                var choices = JanksonUtils.listOfStrings(elementO, "choices");
                choices = choices.size() == 0 ? null : choices;

                OptionGroup.Element<?> element;
                if (choices != null) {
                    element = new OptionGroup.EnumElement(
                        name, (String) defaultValue, nameKey, descriptionKey,
                        prefix, choices
                    );
                }
                else if (defaultValue instanceof Number) {
                    var min = (Number) elementO.get(JsonPrimitive.class, "min").getValue();
                    var max = (Number) elementO.get(JsonPrimitive.class, "max").getValue();
                    if (defaultValue instanceof Double) {
                        element = new OptionGroup.FloatElement(
                            name, (double) defaultValue, nameKey, descriptionKey,
                            (double) min, (double) max
                        );
                    }
                    else if (defaultValue instanceof Long) {
                        element = new OptionGroup.IntegerElement(
                            name, (long) defaultValue, nameKey, descriptionKey,
                            (long) min, (long) max
                        );
                    }
                    else {
                        throw new NotImplementedException();
                    }
                }
                else if (defaultValue instanceof Boolean) {
                    element = new OptionGroup.BooleanElement(name, (boolean) defaultValue, nameKey, descriptionKey);
                }
                else {
                    throw new NotImplementedException();
                }

                elements.put(name, element);
            }
            options.put(includeToken, new OptionGroup(includeToken, categoryKey, elements));
        }

        pipelineJson.remove("options");

        String nameKey = pipelineJson.get(String.class, "nameKey");

        return new PipelineRaw(pipelineLocation, nameKey, options, pipelineJson);
    }

    public OptionGroup.Element<?> optionElementByName(String name) {
        for (var o : options.values()) {
            if (o.elements().containsKey(name)) {
                return o.elements().get(name);
            }
        }
        return null;
    }

    public JsonObject getPipelineJson(Map<OptionGroup.Element<?>, Object> appliedOptions) {
        JsonObject pipelineJson = this.json.clone();

        Function<String, Object> optionValueByName = (String name) -> {
            var element = optionElementByName(name);
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

        return pipelineJson;
    }

}
