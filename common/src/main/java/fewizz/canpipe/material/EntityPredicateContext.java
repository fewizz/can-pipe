package fewizz.canpipe.material;

import net.minecraft.resources.Identifier;

public record EntityPredicateContext(
    Identifier texture,
    String renderLayerName
) {}
