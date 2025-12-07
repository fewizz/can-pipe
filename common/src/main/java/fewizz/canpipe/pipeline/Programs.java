package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.resources.ResourceLocation;


public class Programs {

    private Programs() {}

    static RenderPipeline load(
        JsonObject json,
        ResourceLocation pipelineLocation,
        Function<ResourceLocation, Optional<String>> getShaderSource,
        int glslVersion,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        Optional<Integer> shadowMapSize
    ) {
        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLocation = ResourceLocation.parse(json.get(String.class, "vertexSource"));
        var fragmentLocation = ResourceLocation.parse(json.get(String.class, "fragmentSource"));

        var renderPipelineBuilder = RenderPipeline.builder()
            .withLocation(pipelineLocation.withSuffix("-"+name))
            .withVertexShader(vertexLocation)
            .withFragmentShader(fragmentLocation)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(CanPipe.VertexFormats.POSITION_TEX, VertexFormat.Mode.QUADS);

        renderPipelineBuilder.withUniform("canpipe_ub_pass", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("Projection", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("Fog", UniformType.UNIFORM_BUFFER);

        for (String sampler : samplers) {
            renderPipelineBuilder.withSampler(sampler);
        }

        RenderPipeline pipeline = renderPipelineBuilder.build();

        Function<String, String> postprocess = (src) -> {
            src = src.replaceAll("uniform\\s+ivec2\\s+frxu_size;", "// uniform ivec2 frxu_size;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_lod;", "// uniform int frxu_lod;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_layer;", "// uniform int frxu_layer;");
            src = src.replaceAll("uniform\\s+mat4\\s+frxu_frameProjectionMatrix;", "// uniform mat4 frxu_frameProjectionMatrix;");
            src =
                "layout(std140) uniform canpipe_ub_pass {\n"+
                "    uniform ivec2 frxu_size;\n"+
                "    uniform int frxu_lod;\n"+
                "    uniform int frxu_layer;\n"+
                "    uniform mat4 frxu_frameProjectionMatrix;\n"+
                "};\n\n"+
                src;
            return src;
        };

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineShaderModules(
            pipeline,
            (ResourceLocation location, ShaderType type) -> {
                String src = getShaderSource.apply(location).get();
                return Shaders.process(
                    location, src, type, glslVersion, options, appliedOptions,
                    getShaderSource, shadowMapSize, postprocess
                );
            },
            (String log, ResourceLocation location, String src) -> {
                Path compilationErrorsPath = CanPipe.getCompilationErrorsDirPath();
                try {
                    Files.createDirectories(compilationErrorsPath);
                    Files.writeString(
                        compilationErrorsPath.resolve(location.toDebugFileName()),
                        src+"\n"+log
                    );
                } catch (IOException e) {
                    CanPipe.LOGGER.warn("Couldn't save \""+location.toString()+"\" compilation error", e);
                }
                throw new RuntimeException("Couldn't compile \""+location.toString()+"\": "+log);
            }
        );

        return pipeline;
    }

}
