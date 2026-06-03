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
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderSetup.TextureBinding;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Unique SubmitNodeCollection canpipe_nodeCollectionHeld;

    @Inject(method = "renderSolid", at = @At("HEAD"))
    void onRenderSolid(CallbackInfo ci, @Local(argsOnly = true) SubmitNodeCollection nodeCollection) {
        this.canpipe_nodeCollectionHeld = nodeCollection;
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"))
    void onRenderTranslucent(CallbackInfo ci, @Local(argsOnly = true) SubmitNodeCollection nodeCollection) {
        this.canpipe_nodeCollectionHeld = nodeCollection;
    }

    @Inject(method = "renderModel", at = @At("HEAD"))
    void beforeRenderModel(
        CallbackInfo ci,
        @Local SubmitNodeStorage.ModelSubmit<?> submit,
        @Local(ordinal = 0) VertexConsumer buffer,
        @Local RenderType renderType
    ) {
        TextureAtlasSprite sprite = submit.sprite();

        if (sprite != null) {
            ((VertexConsumerExtended) buffer).canpipe_setScopedSpriteSupplier(() -> sprite);
        }

        Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> extras =
            ((SubmitNodeCollectorExtended) this.canpipe_nodeCollectionHeld).canpipe_getModelSubmitsExtras();
        ModelSubmitExtra extra = extras.get(submit);

        if (extra != null) {
            RenderSetup renderSetup = ((RenderTypeAccessor) renderType).canpipe_getState();
            TextureBinding tex = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getTextures().get("Sampler0");

            var predicateCtx = new EntityMaterialMap.MaterialPedicateContext(
                tex != null ? tex.location() : null,
                extra.spriteId() != null ? extra.spriteId().texture() : null,
                renderType
            );

            EntityMaterialMap materialMap = extra.materialMap();
            Material material;

            if (materialMap == null) {
                material = null;
            }
            else {
                Material foundMaterial = null;
                for (var materialByPredicates : materialMap.materialsByPredicates()) {
                    boolean allTrue = materialByPredicates.predicates().stream().allMatch(p -> p.test(predicateCtx));
                    if (allTrue) {
                        foundMaterial = materialByPredicates.material();
                        break;
                    }
                }
                material = foundMaterial != null ? foundMaterial : materialMap.defaultMaterial();
            }

            ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialSupplier(_sprite -> material);
            ((VertexConsumerExtended) buffer).canpipe_setScopedEntityGlint(extra.entityGlint());
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    void afterRenderModel(CallbackInfo ci, @Local(ordinal = 0) VertexConsumer buffer) {
        ((VertexConsumerExtended) buffer).canpipe_setScopedMaterialSupplier(null);
        ((VertexConsumerExtended) buffer).canpipe_setScopedEntityGlint(false);
        ((VertexConsumerExtended) buffer).canpipe_setScopedSpriteSupplier(null);
    }

}
