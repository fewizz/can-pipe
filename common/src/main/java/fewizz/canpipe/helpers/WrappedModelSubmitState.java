package fewizz.canpipe.helpers;

import fewizz.canpipe.material.EntityMaterialMap;
import net.minecraft.client.resources.model.sprite.SpriteId;

public record WrappedModelSubmitState(
    Object state, SpriteId spriteId, EntityMaterialMap materialMap, boolean entityGlint
) {}
