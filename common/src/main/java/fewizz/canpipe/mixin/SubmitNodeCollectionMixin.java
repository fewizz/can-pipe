package fewizz.canpipe.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.helpers.ModelSubmitExtra;
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeStorage.ModelSubmit;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements SubmitNodeCollectorExtended {

    @Shadow @Final private List<SubmitNodeStorage.ItemSubmit> itemSubmits;
    @Shadow @Final private ModelFeatureRenderer.Storage modelSubmits;

    @Unique private MaterialMap canpipe_pendingItemSubmitMaterialMap = null;
    @Unique private EntityMaterialMap canpipe_scopedModelSubmitMaterialMap = null;
    @Unique private boolean canpipe_pendingModelSubmitEntityGlint = false;
    @Unique private Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_itemSubmitsMaterialMaps;
    @Unique private Map<SubmitNodeStorage.BlockModelSubmit, EntityMaterialMap> canpipe_blockSubmitsMaterialMaps;
    @Unique private Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> canpipe_modelSubmitExtras;
    @Unique private SpriteId canpipe_pendingSpriteId;

    @Override public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingItemSubmitMaterialMap = materialMap; }
    @Override public Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps() { return this.canpipe_itemSubmitsMaterialMaps; }
    @Override public void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap) { canpipe_scopedModelSubmitMaterialMap = materialMap; }
    @Override public void canpipe_setPendingModelEntityGlint() { this.canpipe_pendingModelSubmitEntityGlint = true; }
    @Override public Map<ModelSubmit<?>, ModelSubmitExtra> canpipe_getModelSubmitsExtras() { return this.canpipe_modelSubmitExtras; }
    @Override public void canpipe_setPendingSpriteID(SpriteId pendingSpriteId) { this.canpipe_pendingSpriteId = pendingSpriteId; }
    @Override public Map<SubmitNodeStorage.BlockModelSubmit, EntityMaterialMap> canpipe_getBlockSubmitsMaterialMaps() { return this.canpipe_blockSubmitsMaterialMaps; }

    @Inject(method = "<init>", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        this.canpipe_itemSubmitsMaterialMaps = new HashMap<>();
        this.canpipe_modelSubmitExtras = new HashMap<>();
        this.canpipe_blockSubmitsMaterialMaps = new HashMap<>();
    }

    @Inject(method = "submitItem", at = @At("RETURN"))
    void onItemSubmit(CallbackInfo ci) {
        if (this.canpipe_pendingItemSubmitMaterialMap != null) {
            this.canpipe_itemSubmitsMaterialMaps.put(this.itemSubmits.getLast(), this.canpipe_pendingItemSubmitMaterialMap);
        }
        this.canpipe_pendingItemSubmitMaterialMap = null;
    }

    @Inject(method = "clear", at = @At("RETURN"))
    void onClear(CallbackInfo ci) {
        this.canpipe_pendingItemSubmitMaterialMap = null;  // Should be null here
        this.canpipe_scopedModelSubmitMaterialMap = null;

        this.canpipe_itemSubmitsMaterialMaps.clear();
        this.canpipe_blockSubmitsMaterialMaps.clear();
        this.canpipe_modelSubmitExtras.clear();
    }

    @Inject(method = "submitModel", at = @At("RETURN"))
    void onModelSubmit(CallbackInfo ci, @Local SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        if (this.canpipe_scopedModelSubmitMaterialMap != null || this.canpipe_pendingModelSubmitEntityGlint) {
            this.canpipe_modelSubmitExtras.put(modelSubmit, new ModelSubmitExtra(
                this.canpipe_scopedModelSubmitMaterialMap,
                this.canpipe_pendingSpriteId,
                this.canpipe_pendingModelSubmitEntityGlint
            ));
        }
        this.canpipe_pendingModelSubmitEntityGlint = false;
        this.canpipe_pendingSpriteId = null;
    }

    @ModifyArg(
        method = "submitBlockModel",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        ),
        index = 0
    )
    Object onBlockModelSubmit(Object blockModelSubmit) {
        var submit = (SubmitNodeStorage.BlockModelSubmit) blockModelSubmit;
        if (this.canpipe_scopedModelSubmitMaterialMap != null) {
            this.canpipe_blockSubmitsMaterialMaps.put(submit, this.canpipe_scopedModelSubmitMaterialMap);
        }
        return blockModelSubmit;
    }

}
