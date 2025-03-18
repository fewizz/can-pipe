package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.function.BiFunction;

import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.resources.ResourceLocation;


public class Program extends ProgramBase {

    static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        new ShaderProgramConfig.Uniform("frxu_size", "int", 2, List.of(0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frxu_lod", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frxu_layer", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frxu_frameProjectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F))
    );

    final Uniform FRXU_SIZE;
    final Uniform FRXU_LOD;
    final Uniform FRXU_LAYER;
    final Uniform FRXU_FRAME_PROJECTION_MATRIX;

    final List<String> samplers;

    private Program(String name, List<String> samplers, Shader vertexShader, Shader fragmentShader) {
        super(name, DefaultVertexFormat.POSITION_TEX, List.of(), samplers, DEFAULT_UNIFORMS, vertexShader, fragmentShader);
        this.FRXU_SIZE = this.getUniform("frxu_size");
        this.FRXU_LOD = this.getUniform("frxu_lod");
        this.FRXU_LAYER = this.getUniform("frxu_layer");
        this.FRXU_FRAME_PROJECTION_MATRIX = this.getUniform("frxu_frameProjectionMatrix");
        this.samplers = samplers;
    }

    static Program load(JsonObject json, ResourceLocation pipelineLocation, BiFunction<ResourceLocation, Type, Shader> getOrLoadShader) {
        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLoc = ResourceLocation.parse(json.get(String.class, "vertexSource"));
        var fragmentLoc = ResourceLocation.parse(json.get(String.class, "fragmentSource"));

        Shader vertex = getOrLoadShader.apply(vertexLoc, Type.VERTEX);
        Shader fragment = getOrLoadShader.apply(fragmentLoc, Type.FRAGMENT);

        return new Program(name, samplers, vertex, fragment);
    }

}
