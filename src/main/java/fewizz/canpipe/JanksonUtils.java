package fewizz.canpipe;

import java.util.List;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;

public class JanksonUtils {

    public static void mergeJsonObjectB2A(JsonObject a, JsonObject b) {
        for (var e : b.entrySet()) {
            var k = e.getKey();
            var bv = e.getValue();
            var av = a.get(k);

            if (av != null) {
                if (bv instanceof JsonArray ba) {
                    if (av instanceof JsonArray aa) {
                        aa.addAll(ba);
                        continue;
                    }
                    throw new RuntimeException("Expected array");
                }
                if (bv instanceof JsonObject bo) {
                    if (av instanceof JsonObject ao) {
                        mergeJsonObjectB2A(ao, bo);
                        continue;
                    }
                    throw new RuntimeException("Expected object");
                }
            }

            a.put(k, bv);
        }
    }

    public static List<String> listOfStrings(JsonObject o, String name) {
        JsonArray a = o.get(JsonArray.class, name);
        if (a == null) {
            return List.of();
        }
        return a.stream().map(s -> ((JsonPrimitive)s).asString()).toList();
    }

    public static List<JsonObject> listOfObjects(JsonObject o, String name) {
        JsonArray a = o.get(JsonArray.class, name);
        if (a == null) {
            return List.of();
        }
        return a.stream().map(s -> (JsonObject)s).toList();
    }

}
