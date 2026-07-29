package fewizz.canpipe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fewizz.canpipe.helpers.WrappedListWithExtraElement;
import fewizz.canpipe.helpers.WrappedModelSubmitState;
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements SubmitNodeCollectorExtended {

    @Unique private MaterialMap canpipe_pendingItemSubmitMaterialMap = null;
    @Unique private MaterialMap canpipe_pendingBlockSubmitMaterialMap = null;
    @Unique private EntityMaterialMap canpipe_scopedModelSubmitMaterialMap = null;
    @Unique private boolean canpipe_pendingModelSubmitEntityGlint = false;
    @Unique private SpriteId canpipe_pendingSpriteId;

    @Override public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingItemSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setPendingBlockSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingBlockSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap) { canpipe_scopedModelSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setPendingModelEntityGlint() { this.canpipe_pendingModelSubmitEntityGlint = true; }
    @Override public void canpipe_setPendingSpriteID(SpriteId pendingSpriteId) { this.canpipe_pendingSpriteId = pendingSpriteId; }

    @ModifyArg(
        method = "submitBlockModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/BlockModelFeatureRenderer$Submit;<init>",
            ordinal = 0
        ),
        index = 2
    )
    List<BlockStateModelPart> onBlockModelSubmit(List<BlockStateModelPart> modelParts) {
        if (this.canpipe_pendingBlockSubmitMaterialMap != null) {
            modelParts = new WrappedListWithExtraElement<>(modelParts, this.canpipe_pendingBlockSubmitMaterialMap);
        }
        return modelParts;
    }

    @ModifyArg(
        method = "submitItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;<init>",
            ordinal = 0
        ),
        index = 6
    )
    List<BakedQuad> onItemSubmit(List<BakedQuad> quads) {
        if (this.canpipe_pendingItemSubmitMaterialMap != null) {
            quads = new WrappedListWithExtraElement<>(quads, this.canpipe_pendingItemSubmitMaterialMap);
        }
        return quads;
    }

    @ModifyArg(
        method = "submitModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;<init>",
            ordinal = 0
        ),
        index = 3
    )
    Object onSubmitModel(Object state) {
        if (this.canpipe_scopedModelSubmitMaterialMap != null) {
            state = new WrappedModelSubmitState(
                state, this.canpipe_pendingSpriteId, this.canpipe_scopedModelSubmitMaterialMap, this.canpipe_pendingModelSubmitEntityGlint
            );
        }
        return state;
    }

}
