package fewizz.canpipe.pipeline.load;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.pipeline.OptionGroup;
import fewizz.canpipe.pipeline.RawPipeline;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class RawPipelineLoader {

    public static RawPipeline load(JsonObject json, Identifier location, ResourceManager resourceManager) {

        Map<String, JsonObject> includes = new HashMap<>();

        class ProcessIncludes { private static void doProcess(
            Identifier id,
            JsonObject object,
            Map<String, JsonObject> includes,
            ResourceManager manager
        ) {
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
        ProcessIncludes.doProcess(location, json, includes, resourceManager);

        Map<Identifier, OptionGroup> options = new LinkedHashMap<>();

        for (var optionsA : JanksonUtils.listOfObjects(json, "options")) {
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
                choices = choices.isEmpty() ? null : choices;

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

        json.remove("options");

        String nameKey = json.get(String.class, "nameKey");
        String descriptionKey = json.get(String.class, "descriptionKey");

        var unmodifiableOptions = Collections.unmodifiableMap(options);

        return new RawPipeline(location, json, nameKey, descriptionKey, unmodifiableOptions);

    }

}
