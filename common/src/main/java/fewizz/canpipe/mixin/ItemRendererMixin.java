package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
        method = "getFoilBuffer",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetFoilBuffer(
        MultiBufferSource bufferSource,
        RenderType renderType,
        boolean isItem,
        boolean glint,
        CallbackInfoReturnable<VertexConsumer> cir
    ) {
        // prevent creation of VertexMultiConsumer.Double,
        // pipeline will handle enchanted item glint in material shader
        if (Pipelines.getCurrent() != null && glint) {
            VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
            vce.canpipe_setSharedGlint(true);
            cir.setReturnValue(vce);
        }
    }

    @Inject(
        method = "renderItem",
        at = @At("TAIL")
    )
    private static void onRenderItemEnd(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        if (vertexConsumer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setSharedGlint(false);
            CanPipe.trap();
        }
    }

}
