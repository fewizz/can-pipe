package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;

@Mixin(ChunkSectionLayerGroup.class)
public class ChunkSectionLayerGroupMixin {

    @ModifyExpressionValue(
        method = "outputTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceMainOutputTarget(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.solidFramebuffer);
        }
        return renderTarget;
    }

    @ModifyExpressionValue(
        method = "outputTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getTranslucentTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceTranslucentOutputTarget(RenderTarget renderTarget) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            renderTarget = p.shadowFramebufferOr(p.translucentTerrainFramebuffer);
        }
        return renderTarget;
    }

}
