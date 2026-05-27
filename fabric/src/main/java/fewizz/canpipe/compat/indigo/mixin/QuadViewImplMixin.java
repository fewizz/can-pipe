package fewizz.canpipe.compat.indigo.mixin;

import fewizz.canpipe.material.MaterialMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.compat.indigo.QuadViewExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(value = QuadViewImpl.class, remap = false)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    @Unique protected final float[] canpipe_ao = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    @Unique protected TextureAtlasSprite canpipe_atlasSprite;
    @Unique protected MaterialMap canpipe_materialMap;

    @Override public TextureAtlasSprite canpipe_getSprite() {
        return this.canpipe_atlasSprite;
    }
    @Override public float canpipe_getAO(int index) {
        return this.canpipe_ao[index];
    }
    @Override public MaterialMap canpipe_getMaterialMap() { return this.canpipe_materialMap; }

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
        ((VertexConsumerExtended) vertexConsumer).canpipe_setPendingAO(
            ((QuadViewExtended) this).canpipe_getAO(vertexIndex)
        );
    }

    @Inject(method = "buffer", at = @At("HEAD"))
    void beforeBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        MaterialMap materialMap = ((QuadViewExtended) this).canpipe_getMaterialMap();
        if (materialMap != null) {
            ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
        }
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(() -> ((QuadViewExtended) this).canpipe_getSprite());
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(true);
    }

    @Inject(method = "buffer", at = @At("RETURN"))
    void afterBuffer(CallbackInfo ci, @Local VertexConsumer vertexConsumer) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedSpriteSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_setScopedMaterialSupplier(null);
        ((VertexConsumerExtended) vertexConsumer).canpipe_forceNormalRecomputation(false);
    }

}
