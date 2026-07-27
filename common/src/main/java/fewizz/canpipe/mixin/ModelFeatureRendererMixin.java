package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.WrappedModelSubmitState;
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderSetup.TextureBinding;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @ModifyExpressionValue(
        method = "prepareModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer;getVertexBuilder"
        )
    )
    VertexConsumer onPrepareModel(VertexConsumer vc, @Local ModelFeatureRenderer.Submit<?> submit) {
        if (submit.state() instanceof WrappedModelSubmitState wrapped) {
            EntityMaterialMap materialMap = wrapped.materialMap();
            var vce = (VertexConsumerExtended) vc;

            TextureAtlasSprite sprite = submit.sprite();

            if (sprite != null) {
                vce.canpipe_setScopedSpriteSupplier(() -> sprite);
            }

            var renderType = submit.renderType();
            RenderSetup renderSetup = ((RenderTypeAccessor) renderType).canpipe_getState();
            TextureBinding tex = ((RenderSetupAccessor) (Object) renderSetup).canpipe_getTextures().get("Sampler0");

            Material material;

            if (materialMap == null) {
                material = null;
            }
            else {
                var predicateCtx = new EntityMaterialMap.MaterialPedicateContext(
                    tex != null ? tex.location() : null,
                    wrapped.spriteId() != null ? wrapped.spriteId().texture() : null,
                    renderType
                );
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

            vce.canpipe_setScopedMaterialSupplier(_sprite -> material);
            vce.canpipe_setScopedEntityGlint(wrapped.entityGlint());
        }
        return vc;
    }

    @ModifyExpressionValue(
        method = "prepareModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;state"
        )
    )
    Object fixState(Object state, @Local ModelFeatureRenderer.Submit<?> submit) {
        if (state instanceof WrappedModelSubmitState wrapped) {
            state = wrapped.state();
        }
        return state;
    }

}
