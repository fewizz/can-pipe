package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.state.QuadParticleRenderState;

@Mixin(QuadParticleRenderState.class)
public class QuadParticleRenderStateMixin {

    @ModifyExpressionValue(
        method = "prepare",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/blaze3d/vertex/DefaultVertexFormat;PARTICLE:Lcom/mojang/blaze3d/vertex/VertexFormat;"
        )
    )
    VertexFormat replaceVertexFormat(VertexFormat vertexFormat) {
        assert vertexFormat == DefaultVertexFormat.PARTICLE;
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            vertexFormat = CanPipe.VertexFormats.PARTICLE;
        }
        return vertexFormat;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"
        ),
        index = 0
    )
    RenderPipeline replaceRenderPipeline(RenderPipeline renderPipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderPipeline = p.replaceRenderPipeline(renderPipeline);
        }
        return renderPipeline;
    }

}
