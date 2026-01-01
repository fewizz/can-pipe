package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(
        method = "getFoilBuffer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1 // 0 is `isItem`
    )
    private static boolean onGetFoilBuffer(
        boolean glint,
        @Local(argsOnly = true) MultiBufferSource bufferSource,
        @Local(argsOnly = true) RenderType renderType,
        @Local(argsOnly = true, ordinal = 0) boolean isItem
    ) {
        // prevent creation of VertexMultiConsumer.Double,
        // pipeline will handle enchanted item glint in material shader
        if (Pipelines.getCurrent() != null && glint) {
            VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
            vce.canpipe_setSharedGlint(true);
            glint = false;
        }
        return glint;
    }

    /*@ModifyVariable(
        method = "getArmorFoilBuffer",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static boolean onGetArmorFoilBuffer(
        boolean hasFoil,
        @Local(argsOnly = true) MultiBufferSource bufferSource,
        @Local(argsOnly = true) RenderType renderType
    ) {
        if (Pipelines.getCurrent() != null && hasFoil) {
            VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
            vce.canpipe_setSharedGlint(true);
            hasFoil = false;
        }
        return hasFoil;
    }*/

    @Inject(
        method = "renderItem",
        at = @At("TAIL")
    )
    private static void onRenderItemEnd(
        CallbackInfo ci,
        @Local VertexConsumer vertexConsumer
    ) {
        if (vertexConsumer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setSharedGlint(false);
        }
    }

}
