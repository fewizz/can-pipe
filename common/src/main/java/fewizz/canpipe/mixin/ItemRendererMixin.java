package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"
        )
    )
    private static void beforeRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(argsOnly = true, ordinal = 0) LocalRef<ItemStackRenderState.FoilType> foilType,
        @Local(argsOnly = true) MultiBufferSource bufferSource
    ) {
        if (Pipelines.getCurrent() != null && foilType.get() != ItemStackRenderState.FoilType.NONE) {
            VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
            vce.canpipe_setSharedGlint(true);
            foilType.set(ItemStackRenderState.FoilType.NONE);
        }
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value="INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/resources/model/geometry/BakedQuad;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    private static void afterRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(argsOnly = true) MultiBufferSource bufferSource
    ) {
        VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
        vce.canpipe_setSharedGlint(false);
    }

}
