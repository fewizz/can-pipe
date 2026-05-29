package fewizz.canpipe.helpers;

import org.jspecify.annotations.Nullable;

import fewizz.canpipe.material.EntityMaterialMap;

public record ModelSubmitExtra(@Nullable EntityMaterialMap materialMap, boolean entityGlint) {}
