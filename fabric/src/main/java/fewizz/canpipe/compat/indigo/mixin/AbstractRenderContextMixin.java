package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
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
        VertexConsumer vc,
        CallbackInfo ci
    ) {
        if (
            vc instanceof VertexConsumerExtended vce &&
            vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.SPRITE_INDEX) &&
            quad instanceof MutableQuadViewExtended mq
        ) {
            vce.canpipe_setSpriteSupplier(mq::canpipe_getSprite);
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
        VertexConsumer vc,
        CallbackInfo ci
    ) {
        if (
            vc instanceof VertexConsumerExtended vce &&
            vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.SPRITE_INDEX)
        ) {
            vce.canpipe_setSpriteSupplier(null);
        }
    }

    @Inject(
        method = "bufferQuad("+
            "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    void setExtendedVertexElements(
        MutableQuadViewImpl quad,
        VertexConsumer vc,
        CallbackInfo ci,
        @Local(ordinal = 0) int quadVertexIndex
    ) {
        if (
            vc instanceof VertexConsumerExtended vce &&
            quad instanceof MutableQuadViewExtended q
        ) {
            if (vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.AO)) {
                vce.canpipe_setPendingAO(q.canpipe_getAO(quadVertexIndex));
            }
        }
    }

}
