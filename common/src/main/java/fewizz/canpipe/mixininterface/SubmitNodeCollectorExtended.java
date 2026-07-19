package fewizz.canpipe.mixininterface;

import fewizz.canpipe.material.EntityMaterialMap;
import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.resources.model.sprite.SpriteId;

public interface SubmitNodeCollectorExtended /*extends OrderedSubmitNodeCollector*/ {

    void canpipe_setPendingItemSubmitMaterialMap(MaterialMap materialMap);
    void canpipe_setPendingBlockSubmitMaterialMap(MaterialMap materialMap);

    // Map<SubmitNodeStorage.ItemSubmit, MaterialMap> canpipe_getItemSubmitsMaterialMaps();

    void canpipe_setScopedModelMaterialMap(EntityMaterialMap materialMap);
    void canpipe_setPendingModelEntityGlint();

    // Map<SubmitNodeStorage.ModelSubmit<?>, ModelSubmitExtra> canpipe_getModelSubmitsExtras();

    void canpipe_setPendingSpriteID(SpriteId spriteId);

}
