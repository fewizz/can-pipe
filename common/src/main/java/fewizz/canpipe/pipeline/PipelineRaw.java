package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class PipelineRaw {
    @NotNull public final ResourceLocation location;
    @NotNull public final String nameKey;
    @NotNull public final Map<ResourceLocation, Option> options;
    @NotNull public final JsonObject json;

    PipelineRaw(ResourceLocation location, String nameKey, Map<ResourceLocation, Option> options, JsonObject json) {
        this.location = location;
        this.nameKey = nameKey;
        this.options = options;
        this.json = json;
    }

    static PipelineRaw load(
        JsonObject o,
        ResourceLocation pipelineLocation,
        ResourceManager resourceManager
    ) throws IOException, SyntaxError {
        Map<String, JsonObject> includes = new HashMap<>();

        processIncludes(o, includes, resourceManager);

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
                String descriptionKey = elementO.get(String.class, "descriptionKey");

                var prefix = elementO.get(String.class, "prefix");
                var choices = JanksonUtils.listOfStrings(elementO, "choices");
                choices = choices.size() == 0 ? null : choices;

                Option.Element<?> element;
                if (choices != null) {
                    element = new Option.EnumElement(
                        name, (String) defaultValue, nameKey, descriptionKey,
                        prefix, choices
                    );
                }
                else if (defaultValue instanceof Number) {
                    var min = (Number) elementO.get(JsonPrimitive.class, "min").getValue();
                    var max = (Number) elementO.get(JsonPrimitive.class, "max").getValue();
                    if (defaultValue instanceof Double) {
                        element = new Option.FloatElement(
                            name, (double) defaultValue, nameKey, descriptionKey,
                            (double) min, (double) max
                        );
                    }
                    else if (defaultValue instanceof Long) {
                        element = new Option.IntegerElement(
                            name, (long) defaultValue, nameKey, descriptionKey,
                            (long) min, (long) max
                        );
                    }
                    else {
                        throw new NotImplementedException();
                    }
                }
                else if (defaultValue instanceof Boolean) {
                    element = new Option.BooleanElement(name, (boolean) defaultValue, nameKey, descriptionKey);
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

        return new PipelineRaw(pipelineLocation, nameKey, options, o);
    }

    private static void processIncludes(
        JsonObject object,
        Map<String, JsonObject> includes,
        ResourceManager manager
    ) throws IOException, SyntaxError {
        for (var path : JanksonUtils.listOfStrings(object, "include")) {
            JsonObject toInclude = includes.getOrDefault(path, null);
            if (toInclude == null) {
                toInclude = CanPipe.JANKSON.load(manager.open(ResourceLocation.parse(path)));
                processIncludes(toInclude, includes, manager);
                includes.put(path, toInclude);
            }
            JanksonUtils.mergeJsonObjectB2A(object, toInclude);
        }
    }

}
