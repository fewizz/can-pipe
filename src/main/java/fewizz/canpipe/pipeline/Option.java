package fewizz.canpipe.pipeline;

import java.util.Map;

import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public class Option {
    final ResourceLocation includeToken;
    final Map<String, Option.Element> elements;

    Option(
        ResourceLocation includeToken,
        Map<String, Option.Element> elements
    ) {
        this.includeToken = includeToken;
        this.elements = elements;
    }

    public static class Element {
        final JsonPrimitive defaultValue;
        @Nullable final String prefix;

        Element(JsonPrimitive defaultValue, String prefix) {
            this.defaultValue = defaultValue;
            this.prefix = prefix;
        }
    }
}
