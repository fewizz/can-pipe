package fewizz.canpipe.mixin;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeStorage.ModelSubmit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(SubmitNodeStorage.class)
public class SubmitNodeStorageMixin implements SubmitNodeCollectorExtended {

    @Shadow public SubmitNodeCollection order(final int order) { return null; }

    @Override
    public void canpipe_setPendingItemSubmitExtra(ItemSubmitExtra extra) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setPendingItemSubmitExtra(extra);
    }

    @Override
    public Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_getItemSubmitExtras() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getItemSubmitExtras();
    }

    @Override
    public void canpipe_setModelMaterialMapScope(MaterialMap materialMap) {
        ((SubmitNodeCollectorExtended) this.order(0)).canpipe_setModelMaterialMapScope(materialMap);
    }

    @Override
    public Map<ModelSubmit<?>, MaterialMap> canpipe_getModelsMaterialMaps() {
        return ((SubmitNodeCollectorExtended) this.order(0)).canpipe_getModelsMaterialMaps();
    }

}
