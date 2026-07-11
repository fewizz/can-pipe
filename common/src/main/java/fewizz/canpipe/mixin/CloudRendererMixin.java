package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.CloudRenderer;

// TODO
//@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
/*
    @ModifyExpressionValue(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getCloudsTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceCloudRenderTarget(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.cloudsFramebuffer);
        }
        return renderTarget;
    }
*/
}
