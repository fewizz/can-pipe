package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.ShaderManager.CompilationException;

public abstract class ProgramBase extends GlProgram {

    public final Shader vertexShader;
    public final Shader fragmentShader;

    private static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        new RenderPipeline.UniformDescription("frx_ub_accessibility", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("frx_ub_view", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("frx_ub_player", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("frx_ub_world", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("frx_ub_fog", UniformType.UNIFORM_BUFFER),

        new RenderPipeline.UniformDescription("mc_ub_dynamic_transforms", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("mc_ub_projection", UniformType.UNIFORM_BUFFER),
        new RenderPipeline.UniformDescription("mc_ub_fog", UniformType.UNIFORM_BUFFER)
    );

    private static int _link(String name, GlShaderModule vertexShader, GlShaderModule fragmentShader, VertexFormat vertexFormat, String debugLabel) {
        try {
            return GlProgram.link(vertexShader, fragmentShader, vertexFormat, debugLabel).getProgramId();
        } catch (CompilationException e) {
            throw new RuntimeException("Couldn't link program \""+name+"\": "+e.getMessage(), e);
        }
    }

    ProgramBase(
        String name, VertexFormat vertexFormat,
        List<String> samplers, List<RenderPipeline.UniformDescription> uniforms,
        Shader vertexShader, Shader fragmentShader
    ) {
        super(ProgramBase._link(name, vertexShader, fragmentShader, vertexFormat, name), name);
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        samplers = new ArrayList<>(samplers);
        uniforms = Streams.concat(DEFAULT_UNIFORMS.stream(), uniforms.stream()).toList();

        /*
         * for cases when unform name in `programs` is misspelled, like in:
         * - Aerie v1.0.0 "copy" program
         *   (https://github.com/ambrosia13/Aerie-Shaders/pull/2),
         * - Forget-me-not v0.8.0 "depth_downsample" program
         *   (https://github.com/ambrosia13/ForgetMeNot-Shaders/commit/4eaa1e0f3bec07f265c504d760cccf2676c8fef5)
         */
        /*{
            List<String> activeUniforms = new ArrayList<>();
            List<String> unknownUniforms = new ArrayList<>();
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                IntBuffer size = memoryStack.mallocInt(1);
                IntBuffer type = memoryStack.mallocInt(1);

                int activeUniformsCount = GlStateManager.glGetProgrami(this.getProgramId(), GL33C.GL_ACTIVE_UNIFORMS);
                for (int uniformID = 0; uniformID < activeUniformsCount; uniformID++) {
                    String uniformName = GFX.glGetActiveUniform(this.getProgramId(), uniformID, size, type);
                    activeUniforms.add(uniformName);
                    if (!uniforms.stream().anyMatch(u -> u.name().equals(uniformName)) && !samplers.contains(uniformName)) {
                        unknownUniforms.add(uniformName);
                    }
                }
            }

            for (int i = 0; i < samplers.size(); ++i) {
                var sampler = samplers.get(i);
                if (!activeUniforms.contains(sampler) && unknownUniforms.size() > 0) {
                    String unknownUniform = unknownUniforms.removeFirst();
                    CanPipe.LOGGER.warn("Couldn't find sampler \""+sampler+"\", trying to replace with unknown uniform \""+unknownUniform+"\"");
                    samplers.set(i, unknownUniform);
                }
            }
            this.samplersUniformNames = Collections.unmodifiableList(samplers);
        }*/

        this.setupUniforms(uniforms, samplers);

        // Some dirty aliasing hacks
        var dynamicTransformsUB = getUniforms().remove("mc_ub_dynamic_transforms");
        if (dynamicTransformsUB != null) {
            getUniforms().put("DynamicTransforms", dynamicTransformsUB);
        }

        var projectionUB = getUniforms().remove("mc_ub_projection");
        if (projectionUB != null) {
            getUniforms().put("Projection", projectionUB);
        }

        var fogUB = getUniforms().remove("mc_ub_fog");
        if (fogUB != null) {
            getUniforms().put("Fog", fogUB);
        }
    }

}
