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

    @Unique private MaterialMap canpipe_pendingItemSubmitMaterialMap = null;
    @Unique private MaterialMap canpipe_modelSubmitMaterialMap = null;
    @Unique private Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_itemSubmitsMaterialMaps;
    @Unique private Map<SubmitNodeStorage.ModelSubmit<?>, MaterialMap> canpipe_modelSubmitMaterialMaps;

    @Override public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) { this.canpipe_pendingItemSubmitMaterialMap = materialMap; }
    @Override public Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps() { return this.canpipe_itemSubmitsMaterialMaps; }
    @Override public void canpipe_setModelSumbitMaterialMapScope(MaterialMap materialMap) { canpipe_modelSubmitMaterialMap = materialMap; }
    @Override public Map<ModelSubmit<?>, MaterialMap> canpipe_getModelSubmitsMaterialMaps() { return this.canpipe_modelSubmitMaterialMaps; }

    @Inject(method = "<init>", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        this.canpipe_itemSubmitsMaterialMaps = new HashMap<>();
        this.canpipe_modelSubmitMaterialMaps = new HashMap<>();
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
        canpipe_modelSubmitMaterialMap = null;

        this.canpipe_itemSubmitsMaterialMaps.clear();
        this.canpipe_modelSubmitMaterialMaps.clear();
    }

    @Inject(method = "submitModel", at = @At("RETURN"))
    void onModelSubmit(CallbackInfo ci, @Local SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        if (canpipe_modelSubmitMaterialMap != null) {
            this.canpipe_modelSubmitMaterialMaps.put(modelSubmit, canpipe_modelSubmitMaterialMap);
        }
    }

}
