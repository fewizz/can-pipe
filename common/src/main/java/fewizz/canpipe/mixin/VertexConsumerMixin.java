package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.block.model.BakedQuad;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {

    @Inject(
        method = "putBulkData("+
            "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
            "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
            "[FFFFF[IIZ"+
        ")V",
        at = @At("HEAD")
    )
    default void setSpriteIndex(CallbackInfo ci, @Local(argsOnly = true) BakedQuad bakedQuad) {
        if (this instanceof BufferBuilder bb && bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)) {
            ((VertexConsumerExtended) bb).canpipe_setSpriteSupplier(() -> bakedQuad.getSprite());
        }
    }

    @Inject(
        method = "putBulkData("+
            "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
            "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
            "[FFFFF[IIZ"+
        ")V",
        at = @At("RETURN")
    )
    default void resetSpriteIndex(CallbackInfo ci, @Local(argsOnly = true) BakedQuad bakedQuad) {
        if (this instanceof BufferBuilder bb && bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)) {
            ((VertexConsumerExtended) bb).canpipe_setSpriteSupplier(null);
        }
    }

    @ModifyVariable(
        method = "putBulkData("+
            "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
            "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
            "[FFFFF[IIZ"+
        ")V",
        at = @At(value = "HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    default float[] dontBlendColorWithAO(float[] ao) {
        boolean requiresAO = this instanceof BufferBuilder bb && bb.format.contains(CanPipe.VertexFormatElements.AO);
        return requiresAO ? new float[]{1.0F, 1.0F, 1.0F, 1.0F, ao[0], ao[1], ao[2], ao[3]} : ao;
    }

    @Inject(
        method = "putBulkData("+
            "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
            "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
            "[FFFFF[IIZ"+
        ")V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V", shift = Shift.AFTER)
    )
    default void setAO(
        CallbackInfo ci,
        @Local(ordinal = 0, argsOnly = true) float[] ao,
        @Local(ordinal = 5) int vertexIndex
    ) {
        if (this instanceof BufferBuilder bb) {
            if (bb.format.contains(CanPipe.VertexFormatElements.AO)) {
                ((VertexConsumerExtended) bb).canpipe_setAO(ao[vertexIndex + 4 /* because of mixin above */]);
            }
        }
    }

}
