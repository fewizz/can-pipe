package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;


public class Programs {

    private Programs() {}

    static RenderPipeline load(
        JsonObject json,
        Identifier pipelineLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        int glslVersion,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Optional<Integer> shadowMapSize,
        Pair<List<GpuFormat>, GpuFormat> formats
    ) {
        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLocation = Identifier.parse(json.get(String.class, "vertexSource"));
        var fragmentLocation = Identifier.parse(json.get(String.class, "fragmentSource"));

        var renderPipelineBuilder = RenderPipeline.builder()
            .withLocation(pipelineLocation.withSuffix("-"+name))
            .withVertexShader(vertexLocation)
            .withFragmentShader(fragmentLocation)
            .withCull(false)
            .withVertexBinding(0, CanPipe.VertexFormats.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);

        int colorAttachmentIndex = 0;
        for (var colorAttachmentFormat : formats.getLeft()) {
            renderPipelineBuilder.withColorTargetState(colorAttachmentIndex, new ColorTargetState(
                Optional.empty(),  // no blend function
                colorAttachmentFormat,
                ColorTargetState.WRITE_ALL
            ));
            colorAttachmentIndex += 1;
        }

        var bindGroupLayoutBuilder = BindGroupLayout.builder()
            .withUniform("canpipe_ub_pass", UniformType.UNIFORM_BUFFER)

            .withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_shadow", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);

        for (String sampler : samplers) {
            bindGroupLayoutBuilder.withSampler(sampler);
        }

        renderPipelineBuilder.withBindGroupLayout(bindGroupLayoutBuilder.build());

        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.PROJECTION);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.FOG);

        RenderPipeline pipeline = renderPipelineBuilder.build();

        Function<String, String> postprocess = (src) -> {
            src = src.replaceAll("uniform\\s+ivec2\\s+frxu_size;", "// uniform ivec2 frxu_size;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_lod;", "// uniform int frxu_lod;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_layer;", "// uniform int frxu_layer;");
            src = src.replaceAll("uniform\\s+mat4\\s+frxu_frameProjectionMatrix;", "// uniform mat4 frxu_frameProjectionMatrix;");
            src =
                "layout(std140) uniform canpipe_ub_pass {\n"+
                "    ivec2 frxu_size;\n"+
                "    int frxu_lod;\n"+
                "    int frxu_layer;\n"+
                "    mat4 frxu_frameProjectionMatrix;\n"+
                "};\n\n"+
                src;
            return src;
        };

        TriConsumer<String, Identifier, String> onCompilationError = (String log, Identifier location, String src) -> {
            Path compilationErrorsPath = CanPipe.getCompilationErrorsDirPath();
            try {
                Files.createDirectories(compilationErrorsPath);
                Files.writeString(
                    compilationErrorsPath.resolve(location.toString().replace("/", "--").replace(":", "--")),
                    src+"\n"+log
                );
            } catch (IOException e) {
                CanPipe.LOGGER.warn("Couldn't save \""+location.toString()+"\" compilation error", e);
            }
            throw new RuntimeException("Couldn't compile \""+location.toString()+"\": "+log);
        };

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineModule(
            vertexLocation,
            Shaders.process(
                vertexLocation, getShaderSource.apply(vertexLocation).get(), ShaderType.VERTEX, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ), null, onCompilationError
        );
        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineModule(
            fragmentLocation,
            Shaders.process(
                fragmentLocation, getShaderSource.apply(fragmentLocation).get(), ShaderType.FRAGMENT, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ), null, onCompilationError
        );

        return pipeline;
    }

}
