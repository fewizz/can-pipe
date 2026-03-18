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

    public static List<String> listOfStrings(JsonObject jsonObject, String propertyName) {
        JsonArray a = jsonObject.get(JsonArray.class, propertyName);
        if (a == null) return List.of();
        return a.stream().map(s -> ((JsonPrimitive)s).asString()).toList();
    }

    public static List<Integer> listOfIntegers(JsonObject jsonObject, String propertyName) {
        JsonArray a = jsonObject.get(JsonArray.class, propertyName);
        if (a == null) return List.of();
        return a.stream().map(s -> ((JsonPrimitive)s).asInt(0)).toList();
    }

    public static List<JsonObject> listOfObjects(JsonObject jsonObject, String propertyName) {
        JsonArray a = jsonObject.get(JsonArray.class, propertyName);
        if (a == null) return List.of();
        return a.stream().map(s -> (JsonObject)s).toList();
    }

    public static JsonObject objectOrEmpty(JsonObject jsonObject, String propertyName) {
        JsonObject result = jsonObject.getObject(propertyName);
        return result != null ? result : new JsonObject();
    }

    public static String stringOrThrow(JsonObject jsonObject, String propertyName) {
        String value = jsonObject.get(String.class, propertyName);
        if (value == null) {
            throw new RuntimeException("Couldn't find string property \""+propertyName+"\"");
        }
        return value;
    }

    public static JsonObject objectOrThrow(JsonObject jsonObject, String propertyName) {
        JsonObject value = jsonObject.getObject(propertyName);
        if (value == null) {
            throw new RuntimeException("Couldn't find string property \""+propertyName+"\"");
        }
        return value;
    }

}
