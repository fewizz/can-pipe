package fewizz.canpipe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fewizz.canpipe.helpers.CursedList;
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements SubmitNodeCollectorExtended {
/*
    @Shadow @Final private List<SubmitNodeStorage.ItemSubmit> itemSubmits;
    @Shadow @Final private ModelFeatureRenderer.Storage modelSubmits;
*/
    @Unique private MaterialMap canpipe_pendingItemSubmitMaterialMap = null;
    @Unique private MaterialMap canpipe_pendingBlockSubmitMaterialMap = null;
    @Unique private EntityMaterialMap canpipe_scopedModelSubmitMaterialMap = null;
    @Unique private boolean canpipe_pendingModelSubmitEntityGlint = false;
    // @Unique private Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_itemSubmitsMaterialMaps;
    // @Unique private Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> canpipe_modelSubmitExtras;
    @Unique private SpriteId canpipe_pendingSpriteId;

    @Override public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingItemSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setPendingBlockSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingBlockSubmitMaterialMap = materialMap; }
    // @Override public Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps() { return this.canpipe_itemSubmitsMaterialMaps; }
    @Override public void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap) { canpipe_scopedModelSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setPendingModelEntityGlint() { this.canpipe_pendingModelSubmitEntityGlint = true; }
    // @Override public Map<ModelSubmit<?>, ModelSubmitExtra> canpipe_getModelSubmitsExtras() { return this.canpipe_modelSubmitExtras; }
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
    List<BlockStateModelPart> onSubmitBlockModel(List<BlockStateModelPart> modelParts) {
        if (this.canpipe_pendingBlockSubmitMaterialMap != null) {
            modelParts = new CursedList<>(modelParts, this.canpipe_pendingBlockSubmitMaterialMap);
        }
        return modelParts;
    }


    // @Inject(method = "submitItem", at = @At("RETURN"))
    // void onItemSubmit(CallbackInfo ci) {
    //     if (this.canpipe_pendingItemSubmitMaterialMap != null) {
    //         this.canpipe_itemSubmitsMaterialMaps.put(this.itemSubmits.getLast(), this.canpipe_pendingItemSubmitMaterialMap);
    //     }
    //     this.canpipe_pendingItemSubmitMaterialMap = null;
    // }

    // @Inject(method = "submitModel", at = @At("RETURN"))
    // void onModelSubmit(CallbackInfo ci, @Local SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
    //     if (this.canpipe_scopedModelSubmitMaterialMap != null || this.canpipe_pendingModelSubmitEntityGlint) {
    //         this.canpipe_modelSubmitExtras.put(modelSubmit, new ModelSubmitExtra(
    //             this.canpipe_scopedModelSubmitMaterialMap,
    //             this.canpipe_pendingSpriteId,
    //             this.canpipe_pendingModelSubmitEntityGlint
    //         ));
    //     }
    //     this.canpipe_pendingModelSubmitEntityGlint = false;
    //     this.canpipe_pendingSpriteId = null;
    // }

}
