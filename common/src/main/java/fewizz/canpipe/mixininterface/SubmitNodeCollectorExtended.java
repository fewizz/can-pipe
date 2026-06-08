package fewizz.canpipe.mixininterface;

import java.util.Map;

import fewizz.canpipe.helpers.ModelSubmitExtra;
import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.resources.model.sprite.SpriteId;

public interface SubmitNodeCollectorExtended /*extends OrderedSubmitNodeCollector*/ {

    void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap);
    void canpipe_setPendingBlockSubmitMaterialMap(MaterialMap materialMap);

    Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps();
    Map<SubmitNodeStorage.BlockModelSubmit, MaterialMap> canpipe_getBlockSubmitsMaterialMaps();

    void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap);
    void canpipe_setPendingModelEntityGlint();

    Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> canpipe_getModelSubmitsExtras();

    void canpipe_setPendingSpriteID(SpriteId spriteId);

}
