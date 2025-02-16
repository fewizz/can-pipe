package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.shaders.CompiledShader;

import fewizz.canpipe.CanPipe;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.resources.ResourceLocation;

public class Shader extends CompiledShader {

    static final Predicate<String> CONTAINS_VERTEX_IN = Pattern.compile("\\s*in\\s+vec(3|4)\\s+in_vertex").asPredicate();
    static final Predicate<String> CONTAINS_UV_IN = Pattern.compile("\\s*in\\s+vec2\\s+in_uv").asPredicate();
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

    @SuppressWarnings("unused")
    private final String source;  // for debugging

    private Shader(int id, ResourceLocation resourceLocation, String source) {
        super(id, resourceLocation);
        this.source = source;
    }

    static Shader load(
        ResourceLocation location,
        String source,
        Type type,
        int version,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        Function<ResourceLocation, Optional<String>> getShaderSource,
        @Nullable Framebuffer shadowFramebuffer
    ) throws IOException {
        String preprocessedSource =
            "#version " + version + "\n\n" +
            "#extension GL_ARB_texture_cube_map_array: enable\n\n"+
            "#define " + type.name() + "_SHADER\n\n" +
            (
                shadowFramebuffer != null ?
                "#define SHADOW_MAP_PRESENT\n"+
                "#define SHADOW_MAP_SIZE "+shadowFramebuffer.depthAttachment.texture().extent.x + "\n\n"
                :
                ""
            ) +
            // some shaderpacks define them, some not
            (type == Type.VERTEX && !CONTAINS_VERTEX_IN.test(source) ? "in vec3 in_vertex;\n\n" : "") +
            (type == Type.VERTEX && !CONTAINS_UV_IN.test(source) ? "in vec2 in_uv;\n\n" : "") +
            source;

        preprocessedSource = processIncludesAndDefinitions(
            preprocessedSource, location, options, appliedOptions, getShaderSource
        );

        try {
            return new Shader(CompiledShader.compile(location, type, preprocessedSource).getShaderId(), location, preprocessedSource);
        } catch (CompilationException e) {
            StringBuilder sourceWithLineNumbers = new StringBuilder("\n");
            var lines = preprocessedSource.lines().collect(Collectors.toCollection(ArrayList::new));
            int digits = (int) Math.log10(lines.size()) + 1;
            for (int i = 0; i < lines.size(); ++i) {
                sourceWithLineNumbers.append(("%1$"+digits+"s|").formatted(i+1));
                sourceWithLineNumbers.append(lines.get(i));
                sourceWithLineNumbers.append("\n");
            }
            throw new RuntimeException(new CompilationException(sourceWithLineNumbers.toString()+e.getMessage()));
        }
    }

    private static String processIncludesAndDefinitions(
        String source,
        ResourceLocation sourceLocation,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        Function<ResourceLocation, Optional<String>> getShaderSource
    ) throws IOException {
        Set<ResourceLocation> preprocessed = new HashSet<>();
        Set<String> definedDefinitions = new HashSet<>();

        StringBuilder result = new StringBuilder();

        for (
            Pair<String, Int2BooleanFunction> lc :
            (Iterable<Pair<String, Int2BooleanFunction>>)
            (
                () -> linesIterator(includePreprocessedLinesIterator(
                    source, sourceLocation, preprocessed, options, appliedOptions, getShaderSource
                ))
            )
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
                        .map(o -> o.elements.values()).flatMap(es -> es.stream())
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
                    boolean opResult = false;
                    switch (op) {
                        case ">":
                            opResult = leftF > rightF; break;
                        case "<":
                            opResult = leftF < rightF; break;
                        case "==":
                            opResult = leftF == rightF; break;
                        case "!=":
                            opResult = leftF != rightF; break;
                        default:
                            throw new NotImplementedException(op);
                    }
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
        String source,
        ResourceLocation sourceLocation,
        Set<ResourceLocation> preprocessed,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        Function<ResourceLocation, Optional<String>> getShaderSource
    ) {
        var linesIter = linesIterator(source.lines().iterator());

        return new Iterator<String>() {
            Iterator<String> innerIter = Collections.emptyIterator();

            {
                prepareInnerIter();
            }

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

                var includeMatcher = INCLUDE_PATTERN.matcher(line);

                if (!(includeMatcher.find() && !isCommentedAt.get(includeMatcher.start(1))))  {
                    innerIter = Iterators.singletonIterator(line);  // not an #include
                    return;
                }

                var location = ResourceLocation.parse(includeMatcher.group(1));

                if (preprocessed.contains(location)) {
                    this.prepareInnerIter();
                    return;
                }
                preprocessed.add(location);

                Option option = options.get(location);
                if (option != null) {  // this is an option
                    ArrayList<String> definitions = new ArrayList<>();
                    for (var e : option.elements.entrySet()) {
                        String name = e.getKey();
                        Option.Element<?> element = e.getValue();
                        Object value = appliedOptions.getOrDefault(element, element.defaultValue);

                        String definition = "#define "+name.toUpperCase();

                        if (element instanceof Option.EnumElement enumElement && enumElement.prefix != null) {
                            // define all the variants
                            for (String choice : enumElement.choices) {
                                String defName = enumElement.prefix.toUpperCase()+""+choice.toUpperCase();
                                int valueIndex = enumElement.choices.indexOf(choice);
                                definitions.add("#define "+defName+" "+valueIndex);
                            }
                            definition += " "+enumElement.prefix.toUpperCase()+((String)value).toUpperCase();
                        }
                        else if (element instanceof Option.BooleanElement) {
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
                        CanPipe.LOGGER.warn(sourceLocation+": couldn't include " + location);
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
