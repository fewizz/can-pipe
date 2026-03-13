package fewizz.canpipe.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeStorage.ModelSubmit;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin implements SubmitNodeCollectorExtended {

    @Shadow public SubmitNodeCollection order(final int order) { return null; }

    @Override
    public void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingItemSubmitMaterialMap(materialMap);
    }

    @Override
    public Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getItemSubmitsMaterialMaps();
    }

    @Override
    public void canpipe_setModelSumbitMaterialMapScope(MaterialMap materialMap) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setModelSumbitMaterialMapScope(materialMap);
    }

    @Override
    public Map<ModelSubmit<?>, MaterialMap> canpipe_getModelSubmitsMaterialMaps() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getModelSubmitsMaterialMaps();
    }

}
