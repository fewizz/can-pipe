package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @Inject(
        method = "renderSingleBlock",
        at = @At("HEAD")
    )
    private void beforeModelRender(
        BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
        CallbackInfo ci,
        @Local(argsOnly = true) BlockState bs
    ) {
        if (Pipelines.getCurrent() != null) {
            var vc = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state));
            // ((VertexConsumerExtended) vc).canpipe_recomputeNormal(true);
            if (
                bs != null &&
                vc instanceof BufferBuilder bb &&
                bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                MaterialMap materialMap = MaterialMaps.getForBlock(bs.getBlock());
                ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(materialMap);
            }
        }
    }

    @Inject(
        method = "renderSingleBlock",
        at = @At("TAIL")
    )
    private void afterModelRender(
        BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
        CallbackInfo ci,
        @Local(argsOnly = true) BlockState bs
    ) {
        if (Pipelines.getCurrent() != null) {
            var vc = bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(state));
            // (VertexConsumerExtended) vc).canpipe_recomputeNormal(false);
            if (
                bs != null &&
                vc instanceof BufferBuilder bb &&
                bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(null);
            }
        }
    }

}
