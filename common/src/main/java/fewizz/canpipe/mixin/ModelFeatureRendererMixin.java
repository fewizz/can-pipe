package fewizz.canpipe.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.ModelSubmitExtra;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup.TextureBinding;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Unique SubmitNodeCollection canpipe_nodeCollectionHeld;

    @Inject(method = "renderSolid", at = @At("HEAD"))
    void onRenderSolid(CallbackInfo ci, @Local SubmitNodeCollection nodeCollection) {
        this.canpipe_nodeCollectionHeld = nodeCollection;
    }

    @Inject(method = "renderModel", at = @At("HEAD"))
    void beforeRenderModel(
        CallbackInfo ci,
        @Local SubmitNodeStorage.ModelSubmit<?> submit,
        @Local(ordinal = 0) VertexConsumer buffer,
        @Local RenderType renderType
    ) {

        Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> extras =
            ((SubmitNodeCollectorExtended) this.canpipe_nodeCollectionHeld).canpipe_getModelSubmitsExtras();

        ModelSubmitExtra extra = extras.get(submit);

        if (submit.sprite() == null) {
            RenderSetup renderSetup = ((RenderTypeAccessor) renderType).canpipe_getState();
            TextureBinding tex = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getTextures().get("Sampler0");
            if (tex != null) {
                ((VertexConsumerExtended) buffer).canpipe_setScopedTextureIdentifier(tex.location());
            }
        }
        else {
            ((VertexConsumerExtended) buffer).canpipe_setScopedSpriteSupplier(() -> submit.sprite());
        }

        if (extra != null) {
            ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialMap(extra.materialMap());
            ((VertexConsumerExtended) buffer).canpipe_setScopedEntityGlint(extra.entityGlint());
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    void afterRenderModel(CallbackInfo ci, @Local(ordinal = 0) VertexConsumer buffer) {
        ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialMap(null);
        ((VertexConsumerExtended) buffer).canpipe_setScopedEntityGlint(false);
        ((VertexConsumerExtended) buffer).canpipe_setScopedSpriteSupplier(null);
        ((VertexConsumerExtended) buffer).canpipe_setScopedTextureIdentifier(null);
    }

}
