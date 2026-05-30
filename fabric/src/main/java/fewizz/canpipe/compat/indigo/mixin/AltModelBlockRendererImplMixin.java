package fewizz.canpipe.compat.indigo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AltModelBlockRendererImpl.class)
public class AltModelBlockRendererImplMixin {

    @Shadow @Final private AoCalculator aoCalc;

    @Inject(
        method = "tesselateBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V"
        )
    )
    void beforeEmitQuads(CallbackInfo ci, @Local QuadEmitter output, @Local BlockState blockState) {
        ((MutableQuadViewExtended) output).canpipe_setMaterialMap(MaterialMaps.getForBlockState(blockState));
    }

    @Inject(
        method = "tesselateBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;emitQuads(Lnet/fabricmc/fabric/api/client/renderer/v1/mesh/QuadEmitter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;Ljava/util/function/Predicate;)V",
            shift = Shift.AFTER
        )
    )
    void afterEmitQuads(CallbackInfo ci, @Local QuadEmitter output, @Local BlockState blockState) {
        ((MutableQuadViewExtended) output).canpipe_setMaterialMap(null);
    }

    @Inject(
        method = "shadeQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/fabricmc/fabric/impl/client/indigo/renderer/aocalc/AoCalculator;compute("+
                "Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/QuadViewImpl;"+
                "Z"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void shadeQuad(CallbackInfo ci, @Local MutableQuadViewImpl quad) {
        if (Pipelines.getCurrent() != null) {
            for (int i = 0; i < 4; ++i) {  // AO is handled by pipeline
                ((MutableQuadViewExtended) quad).canpipe_setAO(i, this.aoCalc.ao[i]);
                this.aoCalc.ao[i] = 1.0F;
            }
        }
    }

}
