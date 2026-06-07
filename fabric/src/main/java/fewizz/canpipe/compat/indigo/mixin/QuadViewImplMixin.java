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
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(value = QuadViewImpl.class, remap = false, priority = 1000)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    protected int[] canpipe_quadData;

    @Unique protected TextureAtlasSprite canpipe_atlasSprite;
    @Unique protected Function<TextureAtlasSprite, Material> canpipe_materialSupplier;

    @Override public int[] canpipe_getQuadData() { return this.canpipe_quadData; }
    @Override public void canpipe_setQuadData(int[] data) { this.canpipe_quadData = data; }

    @Shadow protected int baseIndex = 0;

    @ModifyReturnValue(method = "diffuseShade", at = @At("RETURN"))
    boolean hasShade(boolean original) {
        // diffuse lighting is handled by pipeline
        if (Pipelines.getCurrent() != null) {
            return false;
        }
        return original;
    }

    @Inject(
        method = "buffer",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"
        )
    )
    void beforeAddingVertex(
        CallbackInfo ci,
        @Local VertexConsumer vertexConsumer,
        @Local(ordinal = 1) int vertexIndex
    ) {
        int i = (baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 2;
        int ao = (this.canpipe_quadData[i] >>> (vertexIndex * 8)) & 0xFF;
        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingAO(ao / 255.0F);
    }

    @Inject(method = "buffer", at = @At("HEAD"))
    void beforeBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        int spriteIndex = this.canpipe_quadData[(baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 0];
        int materialIndex = this.canpipe_quadData[(baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 1] & 0xFFFF;

        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(this.atlas().getId());
        TextureAtlasSprite sprite = ((TextureAtlasExtended) atlas).canpipe_getSpriteById(spriteIndex);
        Material material = Materials.get((short) materialIndex);

        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(_ -> material);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(() -> sprite);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(true);
    }

    @Inject(method = "buffer", at = @At("RETURN"))
    void afterBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(false);
    }

}
