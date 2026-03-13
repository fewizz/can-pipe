package fewizz.canpipe.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeStorage.ModelSubmit;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin implements SubmitNodeCollectorExtended {

    @Shadow @Final private Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder;

    @Shadow public SubmitNodeCollection order(final int order) { return null; }

    @Unique private MaterialMap canpipe_pendingItemSubmitMaterialMap = null;
    @Unique private MaterialMap canpipe_modelSubmitMaterialMap = null;

    @ModifyReturnValue(method = "lambda$order$0", at = @At("RETURN"))
    SubmitNodeCollection onSubmitNodeCollectionInit(SubmitNodeCollection snc) {
        ((SubmitNodeCollectorExtended) snc).canpipe_setPendingItemSubmitMaterialMap(this.canpipe_pendingItemSubmitMaterialMap);
        ((SubmitNodeCollectorExtended) snc).canpipe_setModelSumbitMaterialMapScope(this.canpipe_modelSubmitMaterialMap);
        return snc;
    }

    @Override
    public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) {
        this.canpipe_pendingItemSubmitMaterialMap = materialMap;
        for (SubmitNodeCollection snc : this.submitsPerOrder.values()) {
            ((SubmitNodeCollectorExtended) snc).canpipe_setPendingItemSubmitMaterialMap(materialMap);
        }
    }

    @Override
    public Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getItemSubmitsMaterialMaps();
    }

    @Override
    public void canpipe_setModelSumbitMaterialMapScope(MaterialMap materialMap) {
        this.canpipe_modelSubmitMaterialMap = materialMap;
        for (SubmitNodeCollection snc : this.submitsPerOrder.values()) {
            ((SubmitNodeCollectorExtended) snc).canpipe_setModelSumbitMaterialMapScope(materialMap);
        }
    }

    @Override
    public Map<ModelSubmit<?>, MaterialMap> canpipe_getModelSubmitsMaterialMaps() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getModelSubmitsMaterialMaps();
    }

}
