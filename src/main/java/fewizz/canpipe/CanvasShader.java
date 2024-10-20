package fewizz.canpipe;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class CanvasShader extends Program {

    private CanvasShader(Type type, int i, ResourceLocation loc) {
        super(type, i, loc.toString());
    }

    public static CanvasShader create(
        Program.Type type,
        ResourceLocation loc,
        int version,
        String source,
        ResourceManager manager,
        Map<ResourceLocation, Option> options
    ) throws IOException {
        String preprocessedSource =
            "#version " + version + "\n\n" +
            "#define " + type.name() + "_SHADER\n\n" +
            source;

        Set<ResourceLocation> preprocessed = new HashSet<>();
        Set<ResourceLocation> processing = new HashSet<>();
        preprocessedSource = getPreprocessedSource(preprocessedSource, preprocessed, processing, manager, options);

        var is = new ByteArrayInputStream(preprocessedSource.getBytes());

        int id = -1;
        try {
            id = Program.compileShaderInternal(type, loc.getPath(), is, loc.getNamespace(), new GlslPreprocessor() {
                @Override
                public String applyImport(boolean bl, String string) {
                    return string;  // we won't handle #moj_import's
                }
            });
        } catch (IOException e) {
            StringBuilder sourceWithLineNumbers = new StringBuilder();
            var lines = preprocessedSource.lines().collect(Collectors.toCollection(ArrayList::new));
            int digits = (int) Math.log10(lines.size()) + 1;
            for(int i = 0; i < lines.size(); ++i) {
                sourceWithLineNumbers.append(("%1$"+digits+"s|").formatted(i+1));
                sourceWithLineNumbers.append(lines.get(i));
                sourceWithLineNumbers.append("\n");
            }
            throw new IOException("Source: \n" + sourceWithLineNumbers.toString(), e);
        }

        CanvasShader shader = new CanvasShader(type, id, loc);
        type.getPrograms().put(shader.getName(), shader);
        return shader;
    }

    private static String getPreprocessedSource(
        String source,
        Set<ResourceLocation> preprocessed,
        Set<ResourceLocation> processing,
        ResourceManager resourceManager,
        Map<ResourceLocation, Option> options
    ) throws IOException {
        StringBuilder preprocessedSource = new StringBuilder();

        Iterable<String> lines = () -> source.lines().iterator();

        for (var line : lines) {
            if (line.startsWith("#include")) {
                line = line.substring("#include".length()).strip();
                var loc = ResourceLocation.parse(line);

                if (!preprocessed.contains(loc)) {
                    Option op = options.get(loc);
                    if (op != null) {
                        StringBuilder optionsDefs = new StringBuilder();
                        for (var e : op.elements.entrySet()) {
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

                            }
                            else {
                                optionsDefs
                                    .append("#define ").append(name.toUpperCase())
                                    .append(" ").append(defaultValue).append("\n");
                            }
                        }
                        line = optionsDefs.toString();
                    }
                    else {
                        if (processing.contains(loc)) {
                            Mod.LOGGER.warn("Circular dependency?: "+ loc.toString());
                            continue;
                        }
                        var resource = resourceManager.getResource(loc);
                        if (resource.isEmpty()) {
                            Mod.LOGGER.warn("couldn't include " + loc);
                            continue;
                        }
                        String resourceStr = resource.get().openAsReader().lines().collect(Collectors.joining("\n"));
                        processing.add(loc);
                        line = getPreprocessedSource(resourceStr, preprocessed, processing, resourceManager, options);
                        processing.remove(loc);
                        preprocessed.add(loc);
                    }
                }
                else { // it was already included
                    continue;
                }
            }
            
            preprocessedSource.append(line).append("\n");
        }

        return preprocessedSource.toString();
    }

}
