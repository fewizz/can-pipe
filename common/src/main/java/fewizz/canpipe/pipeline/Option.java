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
        public final String name;
        public final T defaultValue;
        public final String nameKey;

        Element(String name, T defaultValue, String nameKey) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.nameKey = nameKey;
        }

        public abstract T validate(Object o);

    }

    public static class BooleanElement extends Element<Boolean> {
        BooleanElement(String name, Boolean defaultValue, String nameKey) {
            super(name, defaultValue, nameKey);
        }

        @Override
        public Boolean validate(Object o) {
            return (boolean) o;
        }
    }

    public static class EnumElement extends Element<String> {
        public final String prefix;
        public final List<String> choices;

        EnumElement(String name, String defaultValue, String nameKey, String prefix, List<String> choices) {
            super(name, defaultValue, nameKey);
            this.prefix = prefix;
            this.choices = choices;
        }

        @Override
        public String validate(Object o) {
            return (String) o;
        }
    };

    public static abstract class NumberElement<T extends Number> extends Element<T> {
        public final T min;
        public final T max;

        NumberElement(String name, T defaultValue, String nameKey, T min, T max) {
            super(name, defaultValue, nameKey);
            this.min = min;
            this.max = max;
        }
    }

    public static class FloatElement extends NumberElement<Double> {
        FloatElement(String name, Double defaultValue, String nameKey, Double min, Double max) {
            super(name, defaultValue, nameKey, min, max);
        }

        @Override
        public Double validate(Object o) {
            return (double) o;
        }
    }

    public static class IntegerElement extends NumberElement<Long> {
        IntegerElement(String name, Long defaultValue, String nameKey, Long min, Long max) {
            super(name, defaultValue, nameKey, min, max);
        }

        @Override
        public Long validate(Object o) {
            return (long) o;
        }
    }

}
