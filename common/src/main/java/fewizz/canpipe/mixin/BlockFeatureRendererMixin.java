package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;

// @Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {

    @Unique private static /* !!! */ MaterialMap canpipe_materialMap = null;
/*
    @Inject(
        method = "renderBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setLightCoords(I)V"
        )
    )
    void onRenderBlockModelSubmits(
        CallbackInfo ci,
        @Local SubmitNodeCollection nodeCollection,
        @Local SubmitNodeStorage.BlockModelSubmit submit
    ) {
        canpipe_materialMap = ((SubmitNodeCollectorExtended) nodeCollection).canpipe_getBlockSubmitsMaterialMaps().get(submit);
    }

    @Inject(
        method = "putQuad("+
            "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
            "Lnet/minecraft/client/resources/model/geometry/BakedQuad;"+
            "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            "[I"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
            "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
        ")V",
        at = @At("HEAD")
    )
    private static void onPutQuad(CallbackInfo ci, @Local(ordinal = 0) VertexConsumer buffer, @Local BakedQuad quad) {
        if (buffer instanceof VertexConsumerExtended vce && canpipe_materialMap != null) {
            vce.canpipe_setScopedMaterialSupplier(sprite -> canpipe_materialMap.getMaterial(sprite));
        }

    }
*/
}
