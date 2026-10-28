package fewizz.canpipe.mixin.particles;

import java.util.Map.Entry;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;

@Mixin(QuadParticleFeatureRenderer.class)
public class QuadParticleFeatureRendererMixin {

    @ModifyExpressionValue(
        method = "lambda$prepareGroup$0",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;PARTICLE:Lcom/mojang/renderpearl/api/vertex/VertexFormat;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static VertexFormat replaceVertexFormat(VertexFormat vertexFormat) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            vertexFormat = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0
                ? CanPipe.VertexFormats.PARTICLE_SHADOW
                : CanPipe.VertexFormats.PARTICLE;
        }
        return vertexFormat;
    }

    @WrapOperation(
        method = "drawLayers",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline")
    )
    private static CompiledRenderPipeline replaceRenderPipeline(
        RenderPipeline pipeline,
        Operation<CompiledRenderPipeline> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null && (
            pipeline == RenderPipelines.OPAQUE_PARTICLE ||
            pipeline == RenderPipelines.TRANSLUCENT_PARTICLE
        )) {
            MaterialProgramLoader loader = p.getMaterialProgramLoader(pipeline);
            if (loader != null) {
                Framebuffer fb = p.shadowFramebufferOr(pipeline == RenderPipelines.OPAQUE_PARTICLE ? p.targets.solid() : p.targets.translucentParticles());
                return loader.getOrCreateCompiledRenderPipeline(fb.getFormats());
            }
        }
        return operation.call(pipeline);
    }

    @Inject(
        method = "drawLayers",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setUniform("+
                "Ljava/lang/String;"+
                "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"+
                "Lcom/mojang/renderpearl/api/textures/GpuSampler;"+
            ")V"
        )
    )
    private static void bindSpritesExtentsBeforeRender(
        CallbackInfo ci,
        @Local AbstractTexture texture,
        @Local RenderPass renderPass,
        @Local Entry<SingleQuadParticle.Layer, StagedVertexBuffer.Draw> entry
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            RenderPipeline renderPipeline = entry.getKey().pipeline();
            Pipeline.bindSpritesExtentsSampler(renderPass, texture.getTextureView());
            p.bindUniforms(renderPass, renderPipeline == RenderPipelines.OPAQUE_PARTICLE ? p.targets.solid() : p.targets.translucentParticles(), true);
        }
    }

}
