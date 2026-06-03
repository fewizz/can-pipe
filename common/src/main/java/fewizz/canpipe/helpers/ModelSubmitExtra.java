package fewizz.canpipe.helpers;

import org.jspecify.annotations.Nullable;

import fewizz.canpipe.material.EntityMaterialMap;
import net.minecraft.client.resources.model.sprite.SpriteId;

public record ModelSubmitExtra(
    @Nullable EntityMaterialMap materialMap,
    @Nullable SpriteId spriteId,
    boolean entityGlint
) {}
