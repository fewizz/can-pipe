package fewizz.canpipe.mixin.m04_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    @WrapOperation(
        method = "renderSingleBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;renderModel("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
                "Lnet/minecraft/client/renderer/block/model/BlockStateModel;"+
                "FFFII"+
            ")V"
        )
    )
    private static void beforeModelRender(
        Pose pose, VertexConsumer vc, BlockStateModel bsm, float r, float g, float b, int light, int overlay,
        Operation<Void> operation,
        @Local(argsOnly = true) BlockState bs
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
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
        operation.call(pose, vc, bsm, r, g, b, light, overlay);
        if (p != null) {
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
