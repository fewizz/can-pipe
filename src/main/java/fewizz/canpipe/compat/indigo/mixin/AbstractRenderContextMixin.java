package fewizz.canpipe.compat.indigo.mixin;
/*
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe.VertexFormatElements;
import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;

@Mixin(
    targets="net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractRenderContext",
    remap = false
)*/
public class AbstractRenderContextMixin {
/*
    @Inject(
        method = "bufferQuad("+
            "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At("HEAD")
    )
    void computeTangentIfNeeded(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci
    ) {
        if (
            vertexConsumer instanceof BufferBuilder bb
            && bb.format.contains(CanPipe.VertexFormatElements.TANGENT)
            && quad instanceof MutableQuadViewExtended mq
        ) {
            mq.computeTangent();
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
                ((VertexConsumerExtended) bb).setAO(q.getAO(quadVertexIndex));
            }
            if (bb.format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX)) {
                ((VertexConsumerExtended) bb).setSpriteIndex(q.getSpriteIndex());
            }
            if (bb.format.contains(CanPipe.VertexFormatElements.TANGENT)) {
                ((VertexConsumerExtended) bb).setTangent(q.getTangent());
            }
        }

    }
*/
}
