package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.block.model.multipart.MultiPartModel;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(MultiPartModel.class)
public interface MultiPartModelAccessor {

    @Accessor("blockState")
    BlockState canpipe_getBlockState();

}
