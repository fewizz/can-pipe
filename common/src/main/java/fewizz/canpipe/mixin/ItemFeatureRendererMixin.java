package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.util.WrappedListWithExtraElement;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin extends RenderTypeFeatureRenderer<ItemFeatureRenderer.Submit> {

    @ModifyExpressionValue(
        method = "prepareMainSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder"
        )
    )
    VertexConsumer setMaterial(VertexConsumer vc, @Local ItemFeatureRenderer.Submit submit) {
        if (vc instanceof VertexConsumerExtended vce) {
            if (submit.quads() instanceof WrappedListWithExtraElement cl) {
                MaterialMap materialMap = (MaterialMap) cl.element;
                vce.canpipe_setScopedMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
            }
            if (submit.foilType() != ItemStackRenderState.FoilType.NONE) {
                vce.canpipe_setScopedGlint(true);
            }
        }
        return vc;
    }

    @Inject(
        method = "prepareMainSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad",
            shift = Shift.AFTER
        )
    )
    void cancelGlintAfterFoilSubmitPutQuad(CallbackInfo ci, @Local RenderType renderType) {
        VertexConsumer vc = this.getVertexBuilder(renderType);
        if (vc instanceof VertexConsumerExtended vce) {
            vce.canpipe_setScopedGlint(false);
        }
    }

    @ModifyExpressionValue(
        method = "prepareFoilSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;foilType"
        )
    )
    ItemStackRenderState.FoilType disableVanillaFoil(ItemStackRenderState.FoilType foilType) {
        if (Pipelines.getCurrent() != null) {
            foilType = ItemStackRenderState.FoilType.NONE;
        }
        return foilType;
    }

}
