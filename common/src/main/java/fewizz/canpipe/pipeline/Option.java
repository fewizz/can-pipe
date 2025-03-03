package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

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
        @Nullable public final String descriptionKey;

        Element(String name, T defaultValue, String nameKey, String descriptionKey) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.nameKey = nameKey;
            this.descriptionKey = descriptionKey;
        }

        public abstract T validate(Object o);

    }

    public static class BooleanElement extends Element<Boolean> {

        BooleanElement(String name, Boolean defaultValue, String nameKey, String descriptionKey) {
            super(name, defaultValue, nameKey, descriptionKey);
        }

        @Override
        public Boolean validate(Object o) {
            return (boolean) o;
        }
    }

    public static class EnumElement extends Element<String> {
        public final String prefix;
        public final List<String> choices;

        EnumElement(
            String name, String defaultValue, String nameKey, String descriptionKey,
            String prefix, List<String> choices
        ) {
            super(name, defaultValue, nameKey, descriptionKey);
            Objects.requireNonNull(choices, "\"choices\" is null for enum option element \""+name+"\"");
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

        NumberElement(
            String name, T defaultValue, String nameKey, String descriptionKey,
            T min, T max
        ) {
            super(name, defaultValue, nameKey, descriptionKey);
            this.min = min;
            this.max = max;
        }
    }

    public static class FloatElement extends NumberElement<Double> {

        FloatElement(
            String name, Double defaultValue, String nameKey, String descriptionKey,
            Double min, Double max
        ) {
            super(name, defaultValue, nameKey, descriptionKey, min, max);
        }

        @Override
        public Double validate(Object o) {
            return (double) o;
        }
    }

    public static class IntegerElement extends NumberElement<Long> {

        IntegerElement(
            String name, Long defaultValue, String nameKey, String descriptionKey,
            Long min, Long max
        ) {
            super(name, defaultValue, nameKey, descriptionKey, min, max);
        }

        @Override
        public Long validate(Object o) {
            return (long) o;
        }
    }

}
