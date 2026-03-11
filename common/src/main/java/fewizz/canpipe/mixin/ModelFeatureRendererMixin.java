package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    SubmitNodeCollection canpipe_nodeCollectionHolded;

    @Inject(method = "renderSolid", at = @At("HEAD"))
    void onRenderSolid(CallbackInfo ci, @Local SubmitNodeCollection nodeCollection) {
        this.canpipe_nodeCollectionHolded = nodeCollection;
    }

    @Inject(method = "renderModel", at = @At("HEAD"))
    void beforeRenderModel(
        CallbackInfo ci,
        @Local SubmitNodeStorage.ModelSubmit<?> submit,
        @Local(ordinal = 0) VertexConsumer buffer,
        @Local RenderType renderType
    ) {

        var modelsMaterialMaps = ((SubmitNodeCollectorExtended) this.canpipe_nodeCollectionHolded).canpipe_getModelsMaterialMaps();
        var materialMap = modelsMaterialMaps.get(submit);

        if (submit.sprite() == null) {
            RenderSetup renderSetup = ((RenderTypeAccessor) renderType).canpipe_getState();
            var tex = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getTextures().get("Sampler0");
            ((VertexConsumerExtended) buffer).canpipe_setScopedTextureIdentifier(tex.location());
        }

        if (materialMap != null) {
            ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialMap(materialMap);
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    void afterRenderModel(CallbackInfo ci, @Local(ordinal = 0) VertexConsumer buffer) {
        ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialMap(null);
        ((VertexConsumerExtended) buffer).canpipe_setScopedTextureIdentifier(null);
    }

}
