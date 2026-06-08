package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.block.BlockModelRenderState;

@Mixin(value = BlockModelRenderState.class, priority = 1001)
public interface BlockModelRenderStateAccessor {

    @Accessor("canpipe_materialMap") MaterialMap canpipe_getMaterialMap();
    @Accessor("canpipe_materialMap") void canpipe_setMaterialMap(MaterialMap materialMap);

}
