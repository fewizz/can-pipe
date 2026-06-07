package fewizz.canpipe.compat.indigo.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
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
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(value = QuadViewImpl.class, remap = false)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    @Unique protected final float[] canpipe_ao = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    @Unique protected TextureAtlasSprite canpipe_atlasSprite;
    @Unique protected Function<TextureAtlasSprite, Material> canpipe_materialSupplier;

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
        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingAO(this.canpipe_ao[vertexIndex]);
    }

    @Inject(method = "buffer", at = @At("HEAD"))
    void beforeBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(sprite -> this.canpipe_materialSupplier != null ? this.canpipe_materialSupplier.apply(sprite) : null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(() -> this.canpipe_atlasSprite);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(true);
    }

    @Inject(method = "buffer", at = @At("RETURN"))
    void afterBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(false);
    }

}
