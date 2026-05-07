package fewizz.canpipe.helpers;

import org.jspecify.annotations.Nullable;

import fewizz.canpipe.material.MaterialMap;

public record ModelSubmitExtra(@Nullable MaterialMap materialMap, boolean entityGlint) {}
