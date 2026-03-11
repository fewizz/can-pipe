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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeStorage.ModelSubmit;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements SubmitNodeCollectorExtended {

    @Shadow @Final private List<SubmitNodeStorage.ItemSubmit> itemSubmits;
    @Shadow @Final private ModelFeatureRenderer.Storage modelSubmits;

    @Unique private ItemSubmitExtra canpipe_pendingItemSubmitExtra = null;
    @Unique private MaterialMap canpipe_modelMaterialMap = null;
    @Unique private final Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_itemSubmitExtras = new HashMap<>();
    @Unique private final Map<SubmitNodeStorage.ModelSubmit<?>, MaterialMap> canpipe_modelSubmitMaterialMaps = new HashMap<>();

    @Override public void canpipe_setPendingItemSubmitExtra(ItemSubmitExtra extra) { this.canpipe_pendingItemSubmitExtra = extra; }
    @Override public Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_getItemSubmitExtras() { return this.canpipe_itemSubmitExtras; }
    @Override public void canpipe_setModelMaterialMapScope(MaterialMap materialMap) { this.canpipe_modelMaterialMap = materialMap; }
    @Override public Map<ModelSubmit<?>, MaterialMap> canpipe_getModelsMaterialMaps() { return this.canpipe_modelSubmitMaterialMaps; }

    @Inject(method = "submitItem", at = @At("RETURN"))
    void onItemSubmit(CallbackInfo ci) {
        if (this.canpipe_pendingItemSubmitExtra != null) {
            this.canpipe_itemSubmitExtras.put(this.itemSubmits.getLast(), this.canpipe_pendingItemSubmitExtra);
        }
        this.canpipe_pendingItemSubmitExtra = null;
    }

    @Inject(method = "clear", at = @At("RETURN"))
    void onClear(CallbackInfo ci) {
        this.canpipe_pendingItemSubmitExtra = null;  // Should be null here
        this.canpipe_modelMaterialMap = null;

        this.canpipe_itemSubmitExtras.clear();
        this.canpipe_modelSubmitMaterialMaps.clear();
    }

    @Inject(method = "submitModel", at = @At("RETURN"))
    void onModelSubmit(CallbackInfo ci, @Local SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        if (this.canpipe_modelMaterialMap != null) {
            this.canpipe_modelSubmitMaterialMaps.put(modelSubmit, this.canpipe_modelMaterialMap);
        }
    }

}
