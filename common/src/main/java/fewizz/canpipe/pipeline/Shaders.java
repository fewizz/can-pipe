package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;

import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import net.minecraft.resources.Identifier;

public class Shaders {

    static final Pattern DEFINITION_PATTERN = Pattern.compile("^\\s*#define\\s+([[a-z][A-Z][0-9]_]+)");
    static final Pattern INCLUDE_PATTERN = Pattern.compile("^\\s*#include\\s+([[a-z][0-9]._]+:[[a-z][0-9]._/]+)");

    static final Pattern FLOAT_PATTERN = Pattern.compile("[0-9]+\\.[0-9]+");

    /**
    Naive pattern for matching expressions like <code>#if X op Y</code>
    , where X, Y is either option name or floating-point number,
    op is < > == or !=
    <p>Not supported by GLSL preprocessor, but canvas uses external one that supports this
    */
    static final Pattern FLOAT_CONDITIONAL_PATTERN = Pattern.compile(
        "^\\s*(#if)\\s+("+FLOAT_PATTERN.pattern()+"|[[A-Za-z][0-9]_]+)\\s+([<>]|!=|==)\\s+("+FLOAT_PATTERN.pattern()+"|[[A-Za-z][0-9]_]+)"
    );

    static String process(
        Identifier location, String source, ShaderType type, int version,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<Identifier, Optional<String>> getShaderSource,
        Optional<Integer> shadowMapSize,
        Function<String, String> postProcess
    ) {
        String preprocessedSource = processIncludesAndDefinitions(
            source, location, options, appliedOptions, getShaderSource
        );

        preprocessedSource = postProcess.apply(preprocessedSource);

        String header =
            "#version " + version + "\n\n" +
            "#extension GL_ARB_texture_cube_map_array: enable\n\n"+
            "#define " + type.name() + "_SHADER\n\n";

        if (RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()) {
            header +=
                "#define CANPIPE_Z_ZERO_TO_ONE\n";
        }

        header +=
            "#define CANPIPE_REVERSED_DEPTH\n";

        if (shadowMapSize.isPresent()) {
            header +=
                "#define SHADOW_MAP_PRESENT\n"+
                "#define SHADOW_MAP_SIZE "+shadowMapSize.get()+"\n\n";
        }

        header +=  // LumiLights uses these for variable names
            "#define sample _sample\n"+
            "#define sampler _sampler\n\n";

        header +=  // for ecos
            "#define texture2D texture\n\n";

        return header + preprocessedSource;
    }

    private static String processIncludesAndDefinitions(
        String source,
        Identifier sourceLocation,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<Identifier, Optional<String>> getShaderSource
    ) {
        Set<Identifier> preprocessed = new HashSet<>();
        Set<String> definedDefinitions = new HashSet<>();

        StringBuilder result = new StringBuilder();

        for (
            Pair<String, Int2BooleanFunction> lc :
            (Iterable<Pair<String, Int2BooleanFunction>>)
            () -> linesIterator(includePreprocessedLinesIterator(
                source, sourceLocation, preprocessed, options, appliedOptions, getShaderSource
            ))
        ) {
            String line = lc.getLeft();
            Int2BooleanFunction isCommentedAt = lc.getRight();

            var definitionMatcher = DEFINITION_PATTERN.matcher(line);
            if (definitionMatcher.find() && !isCommentedAt.get(definitionMatcher.start(1))) {
                String definitionName = definitionMatcher.group(1);
                boolean wasAlreadyDefined = !definedDefinitions.add(definitionName);
                if (wasAlreadyDefined) {
                    result.append("#undef "+definitionName+" // canpipe: possible macro redefinition\n");
                }
            }

            // Implemented, because ecos has this conditional: #if HAND_SIZE < 1.0
            var floatConditionalMatcher = FLOAT_CONDITIONAL_PATTERN.matcher(line);
            if (floatConditionalMatcher.find() && !isCommentedAt.get(floatConditionalMatcher.start(1))) {
                Function<String, Object> numByValueOrOption = str -> {
                    if (FLOAT_PATTERN.matcher(str).matches()) {
                        return Double.parseDouble(str);
                    }

                    var e = options.values().stream()
                        .map(o -> o.elements().values()).flatMap(es -> es.stream())
                        .filter(el -> el.name.equals(str.toLowerCase()))
                        .findAny();
                    if (e.isPresent()) {
                        return appliedOptions.getOrDefault(e.get(), e.get().defaultValue);
                    }

                    return null;
                };

                var left = numByValueOrOption.apply(floatConditionalMatcher.group(2));
                String op = floatConditionalMatcher.group(3);
                var right = numByValueOrOption.apply(floatConditionalMatcher.group(4));

                if (left instanceof Double leftF && right instanceof Double rightF) {
                    boolean opResult = switch (op) {
                        case ">" -> leftF > rightF;
                        case "<" -> leftF < rightF;
                        case "==" -> (double) leftF == (double) rightF;
                        case "!=" -> (double) leftF != (double) rightF;
                        default -> throw new NotImplementedException(op);
                    };
                    int conditionalStart = floatConditionalMatcher.start(2);

                    line =
                        line.substring(0, conditionalStart)
                        + (opResult ? "1" : "0")
                        + " // " + line.substring(conditionalStart)
                        + " // canpipe: precomputed";
                }
            }

            result.append(line).append("\n");
        }

        return result.toString();
    }

    private static Iterator<String>
    includePreprocessedLinesIterator(
        String source, Identifier sourceLocation,
        Set<Identifier> preprocessed,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Function<Identifier, Optional<String>> getShaderSource
    ) {
        var linesIter = linesIterator(source.lines().iterator());

        return new Iterator<String>() {
            Iterator<String> innerIter = Collections.emptyIterator();

            { prepareInnerIter(); }

            @Override
            public String next() {
                String result = innerIter.next();
                prepareInnerIter();
                return result;
            }

            @Override
            public boolean hasNext() {
                return innerIter.hasNext();
            }

            public void prepareInnerIter() {
                if (innerIter.hasNext()) {
                    return;
                }
                if (!linesIter.hasNext()) {
                    return;
                }

                var lineAndIsCommentedAt = linesIter.next();

                String line = lineAndIsCommentedAt.getLeft();
                Int2BooleanFunction isCommentedAt = lineAndIsCommentedAt.getRight();

                // MC's shader files always(?) specify this version
                if (line.trim().equals("#version 330") && sourceLocation.getNamespace().equals("minecraft")) {
                    line = "// "+line;
                }

                var includeMatcher = INCLUDE_PATTERN.matcher(line);

                if (!(includeMatcher.find() && !isCommentedAt.get(includeMatcher.start(1))))  {
                    innerIter = Iterators.singletonIterator(line);  // not an #include
                    return;
                }

                var location = Identifier.parse(includeMatcher.group(1));

                if (preprocessed.contains(location)) {
                    this.prepareInnerIter();
                    return;
                }
                preprocessed.add(location);

                OptionGroup option = options.get(location);
                if (option != null) {  // this is an option
                    ArrayList<String> definitions = new ArrayList<>();
                    for (var e : option.elements().entrySet()) {
                        String name = e.getKey();
                        OptionGroup.Element<?> element = e.getValue();
                        Object value = appliedOptions.getOrDefault(element, element.defaultValue);

                        String definition = "#define "+name.toUpperCase();

                        if (element instanceof OptionGroup.EnumElement enumElement && enumElement.prefix != null) {
                            // define all the variants
                            for (String choice : enumElement.choices) {
                                String defName = enumElement.prefix.toUpperCase()+choice.toUpperCase();
                                int valueIndex = enumElement.choices.indexOf(choice);
                                definitions.add("#define "+defName+" "+valueIndex);
                            }
                            definition += " "+enumElement.prefix.toUpperCase()+((String)value).toUpperCase();
                        }
                        else if (element instanceof OptionGroup.BooleanElement) {
                            // don't define if false
                            if ((Boolean) value == false) {
                                continue;
                            }
                        }
                        else {
                            definition += " "+value;
                        }
                        definitions.add(definition);
                    }
                    innerIter = definitions.iterator();
                }
                else {  // this is file include
                    Optional<String> resourceStr = getShaderSource.apply(location);

                    if (resourceStr.isPresent()) {
                        innerIter = includePreprocessedLinesIterator(
                            resourceStr.get(), location,
                            preprocessed, options, appliedOptions, getShaderSource
                        );
                    }
                    else {
                        innerIter = Iterators.singletonIterator("// can-pipe: couldn't include \"" + location + "\"");
                    }
                }

                this.prepareInnerIter();
            }
        };
    }

    /** Iteration element is Pair of:<br>
     * 1. line<br>
     * 2. function, which takes position (int) in the line and returns true
     * if code is commented out at that position, false otherwise
    */
    private static Iterator<Pair<String, Int2BooleanFunction>>
    linesIterator(Iterator<String> lines) {
        return new Iterator<Pair<String, Int2BooleanFunction>>() {
            boolean prevLineIsCommented = false;

            @Override
            public boolean hasNext() {
                return lines.hasNext();
            }

            @Override
            public Pair<String, Int2BooleanFunction> next() {
                String line = lines.next();
                final String initialLine = line;
                final boolean initialPrevLineIsCommented = prevLineIsCommented;
                final int singleLineCommentIndexStart = line.indexOf("//");

                Int2BooleanFunction isMultilineCommentedAt = index -> {
                    boolean comment = initialPrevLineIsCommented;
                    for (int i = 0; i < index; ++i) {
                        if (comment && initialLine.startsWith("*/", i)) {
                            comment = false;
                            ++i;
                        }
                        if (!comment && initialLine.startsWith("/*", i)) {
                            comment = true;
                            ++i;
                        }
                    }
                    return comment;
                };

                Int2BooleanFunction isCommentedAt = index -> {
                    if (singleLineCommentIndexStart != -1 && index > singleLineCommentIndexStart) {
                        return true;
                    }
                    return isMultilineCommentedAt.get(index);
                };

                prevLineIsCommented = isMultilineCommentedAt.get(initialLine.length());

                return Pair.of(line, isCommentedAt);
            }
        };
    }

}
