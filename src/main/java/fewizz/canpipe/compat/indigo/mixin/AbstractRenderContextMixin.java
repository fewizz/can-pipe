package fewizz.canpipe.compat.indigo.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipeVertexFormatElements;
import fewizz.canpipe.CanPipeVertexFormats;
import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewImplExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractRenderContext;

@Mixin(value=AbstractRenderContext.class, remap = false)
public class AbstractRenderContextMixin {

    @Inject(method = "bufferQuad", at = @At("HEAD"))
    void precomputeTangent(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci
    ) {
        if (
            vertexConsumer instanceof BufferBuilder bb
            && bb.format.contains(CanPipeVertexFormatElements.TANGENT)
            && quad instanceof MutableQuadViewImplExtended mq
        ) {
            mq.computeTangent();
        }
    }

    @Inject(
        method = "bufferQuad",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setNormal(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    void setExtendedVertexExelements(
        MutableQuadViewImpl quad,
        VertexConsumer vertexConsumer,
        CallbackInfo ci,
        @Local(ordinal = 0) int quadVertexIndex
    ) {
        if (vertexConsumer instanceof BufferBuilder bb) {
            if (bb.format.contains(CanPipeVertexFormatElements.AO)) {
                float ao = ((MutableQuadViewImplExtended)quad).getAO(quadVertexIndex);
                ((VertexConsumerExtended) bb).setAO(ao);
            }

            if (bb.format.contains(CanPipeVertexFormatElements.SPRITE_INDEX)) {
                int index = ((MutableQuadViewImplExtended)quad).getSpriteIndex();
                ((VertexConsumerExtended) bb).setSpriteIndex(index);
            }

            if (bb.format.contains(CanPipeVertexFormatElements.TANGENT)) {
                int tangent = ((MutableQuadViewImplExtended)quad).getTangent();
                ((VertexConsumerExtended) bb).setTangent(tangent);
            }
        }

    }

}
