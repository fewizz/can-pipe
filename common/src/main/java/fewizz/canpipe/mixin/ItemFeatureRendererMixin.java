package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.WrappedListWithExtraElement;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;

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
        if (submit.quads() instanceof WrappedListWithExtraElement cl) {
            MaterialMap materialMap = (MaterialMap) cl.element;
            ((VertexConsumerExtended) vc).canpipe_setScopedMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
        }
        if (submit.foilType() != ItemStackRenderState.FoilType.NONE) {
            ((VertexConsumerExtended) vc).canpipe_setScopedEntityGlint(true);
        }
        return vc;
    }

    @ModifyExpressionValue(
        method = "prepareFoilSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;foilType"
        )
    )
    ItemStackRenderState.FoilType disableFoilDraw(ItemStackRenderState.FoilType foilType) {
        if (Pipelines.getCurrent() != null) {
            foilType = ItemStackRenderState.FoilType.NONE;
        }
        return foilType;
    }

}
