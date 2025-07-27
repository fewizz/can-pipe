package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.UniformBufferStruct;
import fewizz.canpipe.UniformBufferStruct.IVec2Uniform;
import fewizz.canpipe.UniformBufferStruct.IntUniform;
import fewizz.canpipe.UniformBufferStruct.Mat4Uniform;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import net.minecraft.resources.ResourceLocation;


public class Programs {

    static final UniformBufferStruct PASS = new UniformBufferStruct();
    static final IVec2Uniform FRX_SIZE = PASS.add(new IVec2Uniform());
    static final IntUniform FRX_LOD = PASS.add(new IntUniform());
    static final IntUniform FRX_LAYER = PASS.add(new IntUniform());
    static final Mat4Uniform FRX_FRAME_PROJECTION_MATRIX = PASS.add(new Mat4Uniform());
    static final GpuBuffer PASS_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe view UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, PASS.size()
    );

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
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS);

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

            return
                "#define mc_ub_dynamic_transforms DynamicTransforms\n"+
                "#define mc_ub_projection Projection\n"+
                "#define mc_ub_fog Fog\n"+
                "\n"+
                src;
        };

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipeline(
            pipeline,
            (ResourceLocation location, ShaderType type) -> {
                String src = getShaderSource.apply(location).get();
                return Shaders.process(
                    location, src, type, glslVersion, options, appliedOptions,
                    getShaderSource, shadowMapSize, postprocess
                );
            },
            (String error) -> {
                throw new RuntimeException(error);
            }
        );

        return pipeline;
    }

}
