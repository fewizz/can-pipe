package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.QuadInstanceExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {

    @Inject(method = {"putBlockBakedQuad", "putBakedQuad"}, at = @At("HEAD"))
    default void setSpriteIndex(
        CallbackInfo ci,
        @Local(argsOnly = true) BakedQuad bakedQuad,
        @Local(argsOnly = true) QuadInstance instance
    ) {
        if (this instanceof VertexConsumerExtended vce) {
            if (vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.SPRITE_INDEX)) {
                vce.canpipe_setSpriteSupplier(() -> bakedQuad.spriteInfo().sprite());
            }
            // if (vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.AO)) {
            //     vce.canpipe_setPendingAO(((QuadInstanceExtended) instance).canpipe_getSeparatedShade(vertexIndex));
            // }
        }
    }

    @ModifyArg(
        method = {"putBlockBakedQuad", "putBakedQuad"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;getColor(I)I"
        ),
        index = 0
    )
    default int setAO(int vertexIndex, @Local(argsOnly = true) QuadInstance instance) {
        if (this instanceof VertexConsumerExtended vce) {
            if (vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.AO)) {
                vce.canpipe_setPendingAO(((QuadInstanceExtended) instance).canpipe_getSeparatedShade(vertexIndex));
            }
        }
        return vertexIndex;
    }

    @Inject(method = {"putBlockBakedQuad", "putBakedQuad"}, at = @At("RETURN"))
    default void resetSpriteIndex(CallbackInfo ci, @Local(argsOnly = true) BakedQuad bakedQuad) {
        if (
            this instanceof VertexConsumerExtended vce &&
            vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
        ) {
            vce.canpipe_setSpriteSupplier(null);
        }
    }

}
