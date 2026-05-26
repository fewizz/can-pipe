package fewizz.canpipe.mixininterface;

import org.jspecify.annotations.Nullable;

import fewizz.canpipe.material.Material;

public interface QuadParticleRenderStateStorageExtended {

    public void canpipe_alsoAddMaterial(@Nullable Material material);
    public short[] canpipe_materialsValues();

}
