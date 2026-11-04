package fewizz.canpipe.mixininterface;

import org.jspecify.annotations.Nullable;

import fewizz.canpipe.material.Material;

public interface QuadParticleRenderStateStorageExtended {

    void canpipe_alsoAddMaterial(@Nullable Material material);
    short[] canpipe_materialsValues();

}
