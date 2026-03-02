package fewizz.canpipe.mixin;

import org.apache.commons.lang3.tuple.MutablePair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    ThreadLocal<MutablePair<BlockState, MaterialMap>> canpipe_capturedBlockState = new ThreadLocal<>();

    @Inject(
        method = "compile",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock("+
                "Lnet/minecraft/client/renderer/block/BlockQuadOutput;"+
                "FFF"+
                "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;"+
                "Lnet/minecraft/core/BlockPos;"+
                "Lnet/minecraft/world/level/block/state/BlockState;"+
                "Lnet/minecraft/client/renderer/block/model/BlockStateModel;"+
                "J"+
            ")V"
        )
    )
    void beforeTesselating(CallbackInfoReturnable<SectionCompiler.Results> ci, @Local BlockState blockState) {
        this.canpipe_capturedBlockState.set(MutablePair.of(blockState, null));
    }

    @Inject(
        method = {"lambda$compile$0", "lambda$compile$1"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBlockBakedQuad("+
                "FFF"+
                "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            ")V"
        )
    )
    void beforePuttingQuad(CallbackInfo ci, @Local BufferBuilder bufferBuilder) {
        if (((VertexConsumerExtended) bufferBuilder).canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)) {
            var blockStateAndMaterialMap = this.canpipe_capturedBlockState.get();
            if (blockStateAndMaterialMap.right == null) {
                blockStateAndMaterialMap.right = MaterialMaps.getForBlock(blockStateAndMaterialMap.left.getBlock());
            }

            ((VertexConsumerExtended) bufferBuilder).canpipe_setSharedMaterialMap(blockStateAndMaterialMap.right);
            ((VertexConsumerExtended) bufferBuilder).canpipe_recomputeNormals(true);
        }
    }

    @Inject(
        method = {"lambda$compile$0", "lambda$compile$1"},
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;putBlockBakedQuad("+
                "FFF"+
                "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void afterPuttingQuad(CallbackInfo ci, @Local BufferBuilder bufferBuilder) {
        if (((VertexConsumerExtended) bufferBuilder).canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)) {
            ((VertexConsumerExtended) bufferBuilder).canpipe_setSharedMaterialMap(null);
        }
    }

}
