package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.CursedList;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @ModifyExpressionValue(
        method = "prepareMainSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder"
        )
    )
    VertexConsumer setMaterial(VertexConsumer vc, @Local ItemFeatureRenderer.Submit submit) {
        if (submit.quads() instanceof CursedList cl) {
            MaterialMap materialMap = (MaterialMap) cl.element;
            ((VertexConsumerExtended) vc).canpipe_setScopedMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
        }
        return vc;
    }

/*
    @Unique private MaterialMap canpipe_materialMap = null;

    @WrapOperation(
        method = {"renderSolid", "renderTranslucent"},
        require = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;renderItem("+
                "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"+
                "Lnet/minecraft/client/renderer/OutlineBufferSource;"+
                "Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;"+
            ")V"
        )
    )
    void setItemStack(
        ItemFeatureRenderer instance,
        BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        SubmitNodeStorage.ItemSubmit submit,
        Operation<Void> operation,
        @Local SubmitNodeCollection nodeCollection
    ) {
        try {
            this.canpipe_materialMap = ((SubmitNodeCollectorExtended) nodeCollection).canpipe_getItemSubmitsMaterialMaps().get(submit);
            operation.call(instance, bufferSource, outlineBufferSource, submit);
        } finally {
            this.canpipe_materialMap = null;
        }
    }

    @ModifyExpressionValue(
        method = "renderItem",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;NONE:Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"
        )
    )  // if (foilType != FoilType.NONE) {
    private ItemStackRenderState.FoilType beforeRenderItem(
        ItemStackRenderState.FoilType original,  // NONE
        @Local RenderType renderType,
        @Local(ordinal = 0) ItemStackRenderState.FoilType foilType,
        @Local(argsOnly = true) BufferSource buffer
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return original; }

        VertexConsumer vc = buffer.getBuffer(renderType);
        if (!(vc instanceof VertexConsumerExtended vce)) { return original; }

        if (this.canpipe_materialMap != null) {
            vce.canpipe_setScopedMaterialSupplier(sprite -> this.canpipe_materialMap.getMaterial(sprite));
        }

        if (foilType != ItemStackRenderState.FoilType.NONE) {
            vce.canpipe_setScopedGlint(true);
        }

        return foilType;  // foilType != foilType is always false
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value="INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/resources/model/geometry/BakedQuad;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    private static void afterRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(argsOnly = true) BufferSource buffer
    ) {
        VertexConsumer vc = buffer.getBuffer(renderType);
        if (vc instanceof VertexConsumerExtended vce) {
            vce.canpipe_setScopedGlint(false);
            vce.canpipe_setScopedMaterialSupplier(null);
        }
    }
*/
}
