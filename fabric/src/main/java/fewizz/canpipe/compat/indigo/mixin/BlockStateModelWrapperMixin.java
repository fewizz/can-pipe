package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockStateModelWrapper;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(value = BlockStateModelWrapper.class, priority = 1001 /* after FRAP mixin */)
public class BlockStateModelWrapperMixin {

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads("+
                "Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;"+
                "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"+
                "Lnet/minecraft/core/BlockPos;"+
                "Lnet/minecraft/world/level/block/state/BlockState;"+
                "Lnet/minecraft/util/RandomSource;"+
                "Ljava/util/function/Predicate;"+
            ")V"
        )
    )
    void onUpdate(CallbackInfo ci, @Local QuadEmitter emitter, @Local BlockState blockState) {
        MaterialMap materialMap = MaterialMaps.getForBlockState(blockState);
        if (materialMap != null) {
            ((MutableQuadViewExtended) emitter).canpipe_setMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
        }
    }

}
