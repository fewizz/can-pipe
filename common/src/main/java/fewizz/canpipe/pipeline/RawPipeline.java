package fewizz.canpipe.pipeline;

import java.util.Map;
import java.util.function.Function;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.resources.Identifier;

public class RawPipeline {
    public final Identifier location;
    private final JsonObject json;
    public final String nameKey;
    public final String descriptionKey;
    public final Map<Identifier, OptionGroup> options;
    public final boolean awareOfDepthRangeChanges;

    public RawPipeline(Identifier location, JsonObject json, String nameKey, String descriptionKey, Map<Identifier, OptionGroup> options) {
        this.location = location;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.options = options;
        this.json = json;
        this.awareOfDepthRangeChanges = JanksonUtils.booleanOrDefault(json, "awareOfDepthRangeChanges", false);
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

        this.applyOptionsOnJson(pipelineJson, optionValueByName);

        return pipelineJson;
    }

    private JsonElement applyOptionsOnJson(JsonElement e, Function<String, Object> optionValueByName) {
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
                return vo.get("default");
            }

            var ifDepthIsReversedElement = vo.remove("ifDepthIsReversed");
            if (this.awareOfDepthRangeChanges && ifDepthIsReversedElement != null) {
                if (!(ifDepthIsReversedElement instanceof JsonObject ifDepthIsReversed)) {
                    throw new RuntimeException("Expected value of \"ifDepthIsReversed\" to be an object");
                }
                JanksonUtils.mergeJsonObjectB2A(vo, ifDepthIsReversed);
            }

            for (var kv : vo.entrySet()) {
                kv.setValue(this.applyOptionsOnJson(kv.getValue(), optionValueByName));
            }
        }
        if (e instanceof JsonArray va) {
            for (int i = 0; i < va.size(); ++i) {
                va.set(i, this.applyOptionsOnJson(va.get(i), optionValueByName));
            }
        }
        return e;
    }

}
