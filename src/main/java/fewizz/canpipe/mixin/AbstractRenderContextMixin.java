package fewizz.canpipe.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.VertexFormats;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractBlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AbstractRenderContext;

@Mixin(value=AbstractRenderContext.class, remap = false)
public class AbstractRenderContextMixin {

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
        if (
            vertexConsumer instanceof BufferBuilder bb &&
            bb.format == VertexFormats.BLOCK
        ) {
            if (((Object) this) instanceof AbstractBlockRenderContext brc) {
                AoCalculator aoCalc = ((AbstractBlockRenderContextAccessor) brc).canpipe_getAoCalc();
                bb.setAO(aoCalc.ao[quadVertexIndex]);
            }
            else {
                bb.setAO(1.0F);
            }
            bb.setMaterial(0);
            bb.setTangent(new Vector3f(1.0F, 0.0F, 0.0F));
        }

    }

}
