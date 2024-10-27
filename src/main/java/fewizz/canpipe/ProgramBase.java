package fewizz.canpipe;

import java.io.IOException;
import java.util.List;

import org.joml.Matrix4f;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.resources.ResourceLocation;

public class ProgramBase extends CompiledShaderProgram {

    static final List<ShaderProgramConfig.Uniform> DEFAULT_UNIFORMS = List.of(
        new ShaderProgramConfig.Uniform("frx_viewMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_projectionMatrix", "matrix4x4", 16, List.of(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F)),
        new ShaderProgramConfig.Uniform("frx_modelToCamera_3", "float", 3, List.of(0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogStart", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogEnd", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("frx_fogColor", "float", 4, List.of(0.0F, 0.0F, 0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("_screenSize", "float", 2, List.of(0.0F, 0.0F)),
        new ShaderProgramConfig.Uniform("frx_renderSeconds", "float", 1, List.of(0.0F)),
        new ShaderProgramConfig.Uniform("_frx_renderFrames", "int", 1, List.of(0.0F))
    );

    final Uniform FRX_RENDER_FRAMES;

    ProgramBase(
        ResourceLocation location,
        VertexFormat vertexFormat,
        List<ShaderProgramConfig.Sampler> samplers,
        List<ShaderProgramConfig.Uniform> uniforms,
        Shader vertexShader,
        Shader fragmentShader
    ) throws CompilationException, IOException {
        super(CompiledShaderProgram.link(vertexShader, fragmentShader, vertexFormat).getProgramId());
        setupUniforms(
            Streams.concat(DEFAULT_UNIFORMS.stream(), uniforms.stream()).toList(),
            samplers
        );
        this.FRX_RENDER_FRAMES = getUniform("_frx_renderFrames");
    }

    @Override
    public Uniform getUniform(String name) {
        if (name.equals("ModelViewMat")) { name = "frx_viewMatrix"; }
        if (name.equals("ProjMat")) { name = "frx_projectionMatrix"; }
        if (name.equals("FogStart")) { name = "frx_fogStart"; }
        if (name.equals("FogEnd")) { name = "frx_fogEnd"; }
        if (name.equals("FogColor")) { name = "frx_fogColor"; }
        if (name.equals("ScreenSize")) { name = "_screenSize"; }
        if (name.equals("GameTime")) { name = "frx_renderSeconds"; }

        return super.getUniform(name);
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f matrix4f, Matrix4f matrix4f2, Window window) {
        super.setDefaultUniforms(mode, matrix4f, matrix4f2, window);

        Minecraft mc = Minecraft.getInstance();

        if (this.FRX_RENDER_FRAMES != null) {
            this.FRX_RENDER_FRAMES.set(
                ((GameRendererAccessor) mc.gameRenderer).canpipe_getFrame()
            );
        }
    }

}
