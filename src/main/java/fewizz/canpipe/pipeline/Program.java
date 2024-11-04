package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.resources.ResourceLocation;


public class Program extends ProgramBase {

    static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        new ShaderProgramConfig.Uniform("frxu_size", "int", 2, List.of(0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frxu_lod", "int", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frxu_frameProjectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F))
    );

    final Uniform FRXU_SIZE;
    final Uniform FRXU_LOD;
    final Uniform FRXU_FRAME_PROJECTION_MATRIX;

    public Program(
        ResourceLocation location,
        List<ShaderProgramConfig.Sampler> samplers,
        Shader vertexShader,
        Shader fragmentShader
    ) throws IOException, CompilationException {
        super(
            location,
            DefaultVertexFormat.POSITION_TEX,
            samplers,
            DEFAULT_UNIFORMS,
            vertexShader,
            fragmentShader,
            samplers.stream().map(s -> s.name()).toList()
        );
        this.FRXU_SIZE = getUniform("frxu_size");
        this.FRXU_LOD = getUniform("frxu_lod");
        this.FRXU_FRAME_PROJECTION_MATRIX = getUniform("frxu_frameProjectionMatrix");
    }

    public void setDefaultUniforms(
        Matrix4f view, Matrix4f projection,
        int width, int height,
        int lod
    ) {
        Minecraft mc = Minecraft.getInstance();

        super.setDefaultUniforms(Mode.QUADS, view, projection, mc.getWindow());

        if (FRXU_SIZE != null) {
            FRXU_SIZE.set((int) width, (int) height);
        }
        if (FRXU_LOD != null) {
            FRXU_LOD.set(lod);
        }
        if (FRXU_FRAME_PROJECTION_MATRIX != null) {
            FRXU_FRAME_PROJECTION_MATRIX.set(new Matrix4f().ortho2D(0, width, 0, height));
        }
    }

}
