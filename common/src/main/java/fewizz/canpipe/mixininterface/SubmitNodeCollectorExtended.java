package fewizz.canpipe.mixininterface;

import java.util.Map;

import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.SubmitNodeStorage;

public interface SubmitNodeCollectorExtended /*extends OrderedSubmitNodeCollector*/ {

    void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap);

    Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps();

    void canpipe_setModelSumbitMaterialMapScope(MaterialMap materialMap);

    Map<SubmitNodeStorage.ModelSubmit<?>, MaterialMap> canpipe_getModelSubmitsMaterialMaps();

}
