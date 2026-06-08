package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.material.MaterialMaps;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockStateModelWrapper.class)
public class BlockStateModelWrapperMixin {

    @Inject(method = "update", at = @At("HEAD"))
    void onUpdate(CallbackInfo ci, @Local BlockModelRenderState output, @Local BlockState blockState) {
        ((BlockModelRenderStateAccessor) output).canpipe_setMaterialMap(MaterialMaps.getForBlockState(blockState));
    }

}
