package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractRenderContext;

@Mixin(value = AbstractRenderContext.class)
public class AbstractRenderContextMixin {

    @Inject(
        method = "bufferQuad("+
            "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At("HEAD")
    )
    void setSpriteIndex(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci
    ) {
        if (
            vertexConsumer instanceof BufferBuilder bb
            && bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
            && quad instanceof MutableQuadViewExtended mq
        ) {
            ((VertexConsumerExtended) bb).canpipe_setSpriteSupplier(mq::canpipe_getSprite);
        }
    }

    @Inject(
        method = "bufferQuad("+
            "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At("RETURN")
    )
    void resetSpriteIndex(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci
    ) {
        if (
            vertexConsumer instanceof BufferBuilder bb
            && bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
        ) {
            ((VertexConsumerExtended) bb).canpipe_setSpriteSupplier(null);
        }
    }

    @Inject(
        method = "bufferQuad("+
            "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setNormal(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    void setExtendedVertexElements(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci,
        @Local(ordinal = 0) int quadVertexIndex
    ) {
        if (
            vertexConsumer instanceof BufferBuilder bb &&
            quad instanceof MutableQuadViewExtended q
        ) {
            if (bb.format.contains(CanPipe.VertexFormatElements.AO)) {
                ((VertexConsumerExtended) bb).canpipe_setAO(q.canpipe_getAO(quadVertexIndex));
            }
        }

    }

}
