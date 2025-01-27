package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public class Option {
    public final ResourceLocation includeToken;
    public final String categoryKey;
    public final Map<String, Option.Element<?>> elements;

    Option(
        ResourceLocation includeToken,
        String categoryKey,
        Map<String, Option.Element<?>> elements
    ) {
        this.includeToken = includeToken;
        this.categoryKey = categoryKey;
        this.elements = elements;
    }

    public static abstract class Element<T> {
        public final T defaultValue;
        public final String nameKey;

        Element(T defaultValue, String nameKey) {
            this.defaultValue = defaultValue;
            this.nameKey = nameKey;
        }

    }

    public static class BooleanElement extends Element<Boolean> {
        BooleanElement(Boolean defaultValue, String nameKey) {
            super(defaultValue, nameKey);
        }
    }

    public static class EnumElement extends Element<String> {
        public final String prefix;
        public final List<String> choices;

        EnumElement(String defaultValue, String nameKey, String prefix, List<String> choices) {
            super(defaultValue, nameKey);
            this.prefix = prefix;
            this.choices = choices;
        }
    };

    public static abstract class NumberElement<T extends Number> extends Element<T> {
        public final T min;
        public final T max;

        NumberElement(T defaultValue, String nameKey, T min, T max) {
            super(defaultValue, nameKey);
            this.min = min;
            this.max = max;
        }
    }

    public static class FloatElement extends NumberElement<Double> {
        FloatElement(Double defaultValue, String nameKey, Double min, Double max) {
            super(defaultValue, nameKey, min, max);
        }
    }

    public static class IntegerElement extends NumberElement<Long> {
        IntegerElement(Long defaultValue, String nameKey, Long min, Long max) {
            super(defaultValue, nameKey, min, max);
        }
    }

}
