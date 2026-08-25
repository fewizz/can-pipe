package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.util.WrappedModelSubmitState;
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
        if (vc instanceof VertexConsumerExtended vce && submit.state() instanceof WrappedModelSubmitState wrapped) {
            EntityMaterialMap materialMap = wrapped.materialMap();

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
    Object fixState(Object state) {
        if (state instanceof WrappedModelSubmitState wrapped) {
            state = wrapped.state();
        }
        return state;
    }

    @Inject(
        method = "prepareModel",
        at = @At("RETURN")
    )
    void afterModelPrepared(CallbackInfo ci, @Local VertexConsumer buffer) {
        if (buffer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setScopedMaterialSupplier(null);
            vce.canpipe_setScopedEntityGlint(false);
            vce.canpipe_setScopedSpriteSupplier(null);
        }
    }

}
