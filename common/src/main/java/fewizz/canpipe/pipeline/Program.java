package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.Uniform.Ubo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.UniformBuffer;
import fewizz.canpipe.UniformBuffer.IVec2Uniform;
import fewizz.canpipe.UniformBuffer.IntUniform;
import fewizz.canpipe.UniformBuffer.Mat4Uniform;
import net.minecraft.resources.ResourceLocation;


public class Program extends ProgramBase {

    static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        new RenderPipeline.UniformDescription("canpipe_ub_pass", UniformType.UNIFORM_BUFFER)
    );

    static final UniformBuffer PASS = new UniformBuffer();
    static final IVec2Uniform FRX_SIZE = PASS.add(new IVec2Uniform());
    static final IntUniform FRX_LOD = PASS.add(new IntUniform());
    static final IntUniform FRX_LAYER = PASS.add(new IntUniform());
    static final Mat4Uniform FRX_FRAME_PROJECTION_MATRIX = PASS.add(new Mat4Uniform());
    static final GpuBuffer PASS_UBO = RenderSystem.getDevice().createBuffer(
        () -> "can-pipe view UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, PASS.size()
    );

    public final GlRenderPipeline glRenderPipeline;

    private Program(ResourceLocation pipelineLocation, String name, List<String> samplers, Shader vertexShader, Shader fragmentShader) {
        super(
            name, DefaultVertexFormat.POSITION_TEX,
            samplers, DEFAULT_UNIFORMS,
            vertexShader, fragmentShader
        );

        var renderPipelineBuilder = RenderPipeline.builder()
            .withLocation(pipelineLocation.withSuffix("-"+name))
            .withVertexShader(vertexShader.getId())
            .withFragmentShader(fragmentShader.getId())
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS);

        // not samplers arg, because they can be renamed in super ctor
        // for (var s : this.samplersUniformNames) { renderPipelineBuilder.withSampler(s); }

        for (var s : samplers) { renderPipelineBuilder.withSampler(s); }
        for (var u : this.getUniforms().entrySet()) {
            if (u.getValue() instanceof Ubo) {
                renderPipelineBuilder.withUniform(u.getKey(), UniformType.UNIFORM_BUFFER);
            }
        }

        this.glRenderPipeline = new GlRenderPipeline(renderPipelineBuilder.build(), this);
    }

    static Program load(
        JsonObject json,
        ResourceLocation pipelineLocation,
        Map<Pair<ResourceLocation, ShaderType>, Shader> shaders,
        Function<ResourceLocation, Optional<String>> getShaderSource,
        int glslVersion,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        @Nullable Framebuffer shadowFramebuffer
    ) {
        BiFunction<ResourceLocation, ShaderType, Shader> getOrLoadShader = (ResourceLocation location, ShaderType type) -> {
            return shaders.computeIfAbsent(Pair.of(location, type), locationAndType -> {
                String src = getShaderSource.apply(location).get();

                return Shader.load(location, src, type, glslVersion, options, appliedOptions, getShaderSource, shadowFramebuffer, (s) -> {
                    s = s.replace("uniform ivec2 frxu_size;", "// uniform ivec2 frxu_size;");
                    s = s.replace("uniform int frxu_lod;", "// uniform int frxu_lod;");
                    s = s.replace("uniform int frxu_layer;", "// uniform int frxu_layer;");
                    s = s.replace("uniform mat4 frxu_frameProjectionMatrix;", "// uniform mat4 frxu_frameProjectionMatrix;");
                    s =
                        "layout(std140) uniform canpipe_ub_pass {\n"+
                        "   uniform ivec2 frxu_size;\n"+
                        "   uniform int frxu_lod;\n"+
                        "   uniform int frxu_layer;\n"+
                        "   uniform mat4 frxu_frameProjectionMatrix;\n"+
                        "};\n\n"+
                        s;
                    return s;
                });
            });
        };

        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLoc = ResourceLocation.parse(json.get(String.class, "vertexSource"));
        var fragmentLoc = ResourceLocation.parse(json.get(String.class, "fragmentSource"));

        Shader vertex = getOrLoadShader.apply(vertexLoc, ShaderType.VERTEX);
        Shader fragment = getOrLoadShader.apply(fragmentLoc, ShaderType.FRAGMENT);

        return new Program(pipelineLocation, name, samplers, vertex, fragment);
    }

}
