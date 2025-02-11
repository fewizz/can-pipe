package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

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

    @SuppressWarnings("unused")
    private final String source;  // for debug

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

        Set<ResourceLocation> preprocessed = new HashSet<>();
        Set<ResourceLocation> processing = new HashSet<>();
        Set<String> definitions = new HashSet<>();

        preprocessedSource = processIncludesAndDefinitions(
            preprocessedSource, location, preprocessed, processing, options, appliedOptions, definitions, getShaderSource
        );

        try {
            return new Shader(CompiledShader.compile(location, type, preprocessedSource).getShaderId(), location, preprocessedSource);
        } catch (CompilationException e) {
            StringBuilder sourceWithLineNumbers = new StringBuilder();
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
        Set<ResourceLocation> preprocessed,
        Set<ResourceLocation> processing,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        Set<String> definedDefinitions,
        Function<ResourceLocation, Optional<String>> getShaderSource
    ) throws IOException {
        // includes
        ArrayList<String> linesIncludeProcessed = new ArrayList<>();
        Iterable<Pair<String, Int2BooleanFunction>> lines = () -> forEachLine(source.lines().iterator());

        for (Pair<String, Int2BooleanFunction> lc : lines) {
            String line = lc.getKey();
            Int2BooleanFunction isCommentedAt = lc.getValue();

            var includeMatcher = INCLUDE_PATTERN.matcher(line);

            if (!(includeMatcher.find() && !isCommentedAt.get(includeMatcher.start(1))))  {
                linesIncludeProcessed.add(line);
                continue;  // not an #include
            }

            String locationStr = includeMatcher.group(1);
            var location = ResourceLocation.parse(locationStr);

            if (preprocessed.contains(location)) {
                continue;  // already included
            }

            Option option = options.get(location);
            if (option != null) {  // this is an option
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
                            linesIncludeProcessed.add("#define "+defName+" "+valueIndex);
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

                    linesIncludeProcessed.add(definition);
                }
            }
            else if (!processing.contains(location)) {  // this is file include
                Optional<String> resourceStr = getShaderSource.apply(location);

                if (resourceStr.isPresent()) {
                    processing.add(location);
                    linesIncludeProcessed.add(
                        processIncludesAndDefinitions(resourceStr.get(), location, preprocessed, processing, options, appliedOptions, definedDefinitions, getShaderSource)
                    );
                    processing.remove(location);
                    preprocessed.add(location);
                }
                else {
                    CanPipe.LOGGER.warn(sourceLocation+": couldn't include " + location);
                }
            }
            else {
                // Mod.LOGGER.warn("Circular dependency?: "+ loc.toString());
            }
        }

        // definitions
        StringBuilder preprocessedSource = new StringBuilder();
        lines = () -> forEachLine(linesIncludeProcessed.iterator());

        for (Pair<String, Int2BooleanFunction> lc : lines) {
            String line = lc.getKey();
            Int2BooleanFunction isCommentedAt = lc.getValue();

            var definitionMatcher = DEFINITION_PATTERN.matcher(line);
            if (definitionMatcher.find() && !isCommentedAt.get(definitionMatcher.start(1))) {
                String definitionName = definitionMatcher.group(1);
                boolean redefine = !definedDefinitions.add(definitionName);
                if (redefine) {
                    line = "#undef "+definitionName+"  // canpipe: redefining\n" + line;
                }
            }

            preprocessedSource.append(line).append("\n");
        }

        return preprocessedSource.toString();
    }

    private static Iterator<Pair<String, Int2BooleanFunction>>
    forEachLine(Iterator<String> lines) {
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
