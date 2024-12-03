package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.mojang.blaze3d.shaders.CompiledShader;

import fewizz.canpipe.Mod;
import it.unimi.dsi.fastutil.ints.Int2BooleanFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class Shader extends CompiledShader {

    static final Predicate<String> CONTAINS_VERTEX_IN = Pattern.compile("\\s*in\\s+vec(3|4)\\s+in_vertex").asPredicate();
    static final Predicate<String> CONTAINS_UV_IN = Pattern.compile("\\s*in\\s+vec2\\s+in_uv").asPredicate();
    static final Pattern DEFINITION_PATTERN = Pattern.compile("^\\s*#define\\s+([[a-z][A-Z][0-9]_]+)");
    static final Pattern INCLUDE_PATTERN = Pattern.compile("^\\s*#include\\s+([[a-z][0-9]._]+:[[a-z][0-9]._/]+)");

    private Shader(int id, ResourceLocation resourceLocation) {
        super(id, resourceLocation);
    }

    public static Shader compile(
        ResourceLocation location,
        Type type,
        int version,
        Map<ResourceLocation, Option> options,
        String source
    ) throws CompilationException, IOException {
        String preprocessedSource =
            "#version " + version + "\n\n" +
            "#define " + type.name() + "_SHADER\n\n" +
            // some shaderpacks define them, some not
            (type == Type.VERTEX && !CONTAINS_VERTEX_IN.test(source) ? "in vec3 in_vertex;\n\n" : "") +
            (type == Type.VERTEX && !CONTAINS_UV_IN.test(source) ? "in vec2 in_uv;\n\n" : "") +
            source;

        Set<ResourceLocation> preprocessed = new HashSet<>();
        Set<ResourceLocation> processing = new HashSet<>();
        Set<String> definitions = new HashSet<>();

        preprocessedSource = processIncludes(preprocessedSource, preprocessed, processing, options, definitions);

        try {
            return new Shader(CompiledShader.compile(location, type, preprocessedSource).getShaderId(), location);
        } catch (CompilationException e) {
            StringBuilder sourceWithLineNumbers = new StringBuilder();
            var lines = preprocessedSource.lines().collect(Collectors.toCollection(ArrayList::new));
            int digits = (int) Math.log10(lines.size()) + 1;
            for(int i = 0; i < lines.size(); ++i) {
                sourceWithLineNumbers.append(("%1$"+digits+"s|").formatted(i+1));
                sourceWithLineNumbers.append(lines.get(i));
                sourceWithLineNumbers.append("\n");
            }
            throw new CompilationException(sourceWithLineNumbers.toString()+e.getMessage());
        }
    }

    private static String processIncludes(
        String source,
        Set<ResourceLocation> preprocessed,
        Set<ResourceLocation> processing,
        Map<ResourceLocation, Option> options,
        Set<String> definitions
    ) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        ResourceManager resourceManager = mc.getResourceManager();

        StringBuilder preprocessedSource = new StringBuilder();

        Iterable<String> lines = () -> source.lines().iterator();

        boolean prevWasComment = false;

        for (var line : lines) {
            final var initialLine = line;
            final var initialPrevWasComment = prevWasComment;
            int singleLineCommentIndex = line.indexOf("//");

            Int2BooleanFunction isMultilineComment = index -> {
                boolean comment = initialPrevWasComment;
                for (int i = 0; i < Math.min(index, initialLine.length()-2); ++i) {
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

            Int2BooleanFunction isComment = index -> {
                if (singleLineCommentIndex != -1 && index > singleLineCommentIndex) {
                    return true;
                }
                return isMultilineComment.get(index);
            };

            var definitionMatcher = DEFINITION_PATTERN.matcher(line);
            if (definitionMatcher.find() && !isComment.get(definitionMatcher.start(1))) {
                String definitionName = definitionMatcher.group(1);
                boolean redefine = !definitions.add(definitionName);
                if (redefine) {
                    line = "#undef "+definitionName+"// canpipe: `undef`ined\n"+line;
                }
            }

            var includeMatcher = INCLUDE_PATTERN.matcher(line);
            if (includeMatcher.find() && !isComment.get(includeMatcher.start(1))) {
                String locStr = includeMatcher.group(1);
                var loc = ResourceLocation.parse(locStr);

                if (!preprocessed.contains(loc)) {
                    Option option = options.get(loc);
                    if (option != null) {
                        StringBuilder optionsDefs = new StringBuilder();
                        for (var e : option.elements.entrySet()) {
                            String name = e.getKey();
                            Option.Element value = e.getValue();
                            var defaultValue = value.defaultValue.getValue();
                            if (defaultValue instanceof Boolean) {
                                if (defaultValue == Boolean.TRUE) {
                                    optionsDefs
                                        .append("#define ")
                                        .append(name.toUpperCase())
                                        .append("\n");
                                }
                                definitions.add(name.toUpperCase());
                            }
                            else {
                                optionsDefs
                                    .append("#define ").append(name.toUpperCase())
                                    .append(" ").append(defaultValue).append("\n");
                                definitions.add(name.toUpperCase());
                            }
                        }
                        line = optionsDefs.toString();
                    }
                    else if (!processing.contains(loc)) {
                        var resource = resourceManager.getResource(loc);
                        if (!resource.isEmpty()) {
                            String resourceStr = resource.get().openAsReader().lines().collect(Collectors.joining("\n"));
                            processing.add(loc);
                            line = processIncludes(resourceStr, preprocessed, processing, options, definitions);
                            processing.remove(loc);
                            preprocessed.add(loc);
                        }
                        else {
                            Mod.LOGGER.warn("Couldn't include " + loc);
                            line = "";
                        }
                    }
                    else {
                        // Mod.LOGGER.warn("Circular dependency?: "+ loc.toString());
                        line = "";
                    }
                }
                else {  // already included
                    line = "";
                }
            }

            preprocessedSource.append(line).append("\n");
            prevWasComment = isMultilineComment.get(initialLine.length()-1);
        }

        return preprocessedSource.toString();
    }

}
