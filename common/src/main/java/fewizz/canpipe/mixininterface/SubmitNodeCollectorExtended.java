package fewizz.canpipe.mixininterface;

import java.util.Map;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.SubmitNodeStorage;

public interface SubmitNodeCollectorExtended /*extends OrderedSubmitNodeCollector*/ {

    void canpipe_setPendingItemSubmitExtra(ItemSubmitExtra extra);

    Map<SubmitNodeStorage.ItemSubmit, ItemSubmitExtra> canpipe_getItemSubmitExtras();

    void canpipe_setModelMaterialMapScope(MaterialMap materialMap);

    Map<SubmitNodeStorage.ModelSubmit<?>, MaterialMap> canpipe_getModelsMaterialMaps();

}
