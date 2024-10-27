package fewizz.canpipe;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.blaze3d.shaders.CompiledShader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class Shader extends CompiledShader {

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
            source;

        Set<ResourceLocation> preprocessed = new HashSet<>();
        Set<ResourceLocation> processing = new HashSet<>();
        preprocessedSource = processIncludes(preprocessedSource, preprocessed, processing, options);
        return new Shader(CompiledShader.compile(location, type, preprocessedSource).getShaderId(), location);
    }

    private static String processIncludes(
        String source,
        Set<ResourceLocation> preprocessed,
        Set<ResourceLocation> processing,
        Map<ResourceLocation, Option> options
    ) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        ResourceManager resourceManager = mc.getResourceManager();

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
                        line = processIncludes(resourceStr, preprocessed, processing, options);
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
