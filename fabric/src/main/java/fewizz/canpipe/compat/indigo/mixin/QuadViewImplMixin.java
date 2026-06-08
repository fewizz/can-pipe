package fewizz.canpipe.compat.indigo.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.compat.indigo.QuadViewExtended;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(value = QuadViewImpl.class, remap = false, priority = 1000)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    @Shadow protected int[] data;
    @Shadow protected int baseIndex = 0;

    @Unique protected int[] canpipe_extraData;

    @Unique protected Function<TextureAtlasSprite, Material> canpipe_materialSupplier;

    @Override public int canpipe_getBaseIndex() { return this.baseIndex; }
    @Override public int[] canpipe_getExtraData() { return this.canpipe_extraData; }
    @Override public void canpipe_setExtraData(int[] data) { this.canpipe_extraData = data; }

    @ModifyReturnValue(method = "diffuseShade", at = @At("RETURN"))
    boolean hasShade(boolean original) {
        // diffuse lighting is handled by pipeline
        if (Pipelines.getCurrent() != null) {
            return false;
        }
        return original;
    }

    @Inject(
        method = "buffer*",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"
        )
    )
    void beforeAddingVertex(
        CallbackInfo ci,
        @Local VertexConsumer vertexConsumer,
        @Local(name = "i") int vertexIndex
    ) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        int ao = (this.canpipe_extraData[i+2] >>> (vertexIndex * 8)) & 0xFF;
        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingAO(ao / 255.0F);
    }

    @Inject(method = "buffer*", at = @At("HEAD"))
    void beforeBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        int spriteIndex = this.canpipe_extraData[i+0];
        short materialIndex = (short) (this.canpipe_extraData[i+1] & 0xFFFF);

        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingSpriteIndex(spriteIndex);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingMaterialIndex(materialIndex);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(true);
    }

    @Inject(method = "buffer*", at = @At("RETURN"))
    void afterBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(false);
    }

}
