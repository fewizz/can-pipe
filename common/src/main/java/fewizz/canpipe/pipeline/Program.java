package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.function.BiFunction;

import org.joml.Matrix4f;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;


public class Program extends ProgramBase {

    static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        new RenderPipeline.UniformDescription("frxu_size", UniformType.valueOf("IVEC2")),
        new RenderPipeline.UniformDescription("frxu_lod", UniformType.INT),
        new RenderPipeline.UniformDescription("frxu_layer", UniformType.INT),
        new RenderPipeline.UniformDescription("frxu_frameProjectionMatrix", UniformType.MATRIX4X4)
    );

    public final RenderPipeline renderPipeline;

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

        for (var s : samplers) { renderPipelineBuilder.withSampler(s); }

        this.renderPipeline = renderPipelineBuilder.build();
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f viewMatrix, Matrix4f projectionMatrix, float w, float h) {
        Minecraft mc = Minecraft.getInstance();
        GameRendererExtended gre = (GameRendererExtended) mc.gameRenderer;
        super.setDefaultUniforms(mode, gre.canpipe_getViewMatrix(), gre.canpipe_getProjectionMatrix(), w, h);
    }

    static Program load(JsonObject json, ResourceLocation pipelineLocation, BiFunction<ResourceLocation, ShaderType, Shader> getOrLoadShader) {
        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLoc = ResourceLocation.parse(json.get(String.class, "vertexSource"));
        var fragmentLoc = ResourceLocation.parse(json.get(String.class, "fragmentSource"));

        Shader vertex = getOrLoadShader.apply(vertexLoc, ShaderType.VERTEX);
        Shader fragment = getOrLoadShader.apply(fragmentLoc, ShaderType.FRAGMENT);

        return new Program(pipelineLocation, name, samplers, vertex, fragment);
    }

}
