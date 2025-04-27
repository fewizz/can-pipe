package fewizz.canpipe.mixin.m04_core;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    // Shading is controlled by pipeline
    @ModifyExpressionValue(
        method = {"renderModelFaceAO", "renderModelFaceFlat"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;shade()Z"
        )
    )
    boolean dontShade(boolean shade) {
        return shade && Pipelines.getCurrent() == null;
    }

    @Inject(
        method = "putQuadData",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
                "[FFFFF[IIZ"+
            ")V"
        )
    )
    void onBeforeData(
        CallbackInfo ci,
        @Local(argsOnly = true) BlockState bs,
        @Local(argsOnly = true) VertexConsumer vc
    ) {
        if (Pipelines.getCurrent() != null) {
            ((VertexConsumerExtended) vc).canpipe_recomputeNormal(true);

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
        method = "putQuadData",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
                "[FFFFF[IIZ"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void onAfterData(
        CallbackInfo ci,
        @Local(argsOnly = true) BlockState bs,
        @Local(argsOnly = true) VertexConsumer vc
    ) {
        if (Pipelines.getCurrent() != null) {
            ((VertexConsumerExtended) vc).canpipe_recomputeNormal(false);

            if (
                bs != null &&
                vc instanceof BufferBuilder bb &&
                bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(null);
            }
        }
    }

    /*@Inject(
        method = "renderModel",
        at = @At("HEAD")
    )
    private static void onBeforeSingleBlockData(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumer vc,
        @Local(argsOnly = true) @Nullable BlockState bs
    ) {
        if (Pipelines.getCurrent() != null) {
            ((VertexConsumerExtended) vc).canpipe_recomputeNormal(true);

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
        method = "renderModel",
        at = @At("TAIL")
    )
    private static void onAfterSingleBlockData(
        CallbackInfo ci,
        @Local(argsOnly = true) VertexConsumer vc,
        @Local(argsOnly = true) @Nullable BlockState bs
    ) {
        if (Pipelines.getCurrent() != null) {
            ((VertexConsumerExtended) vc).canpipe_recomputeNormal(false);

            if (
                bs != null &&
                vc instanceof BufferBuilder bb &&
                bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(null);
            }
        }
    }*/

}
