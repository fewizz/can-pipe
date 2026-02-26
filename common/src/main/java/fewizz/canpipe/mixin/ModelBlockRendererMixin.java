package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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

    /*@Inject(
        method = "putQuadData",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
                "[FFFFF[II"+
            ")V"
        )
    )
    void onBeforeData(
        CallbackInfo ci,
        @Local(argsOnly = true) BlockState bs,
        @Local(argsOnly = true) VertexConsumer vc
    ) {
        if (Pipelines.getCurrent() != null) {
            VertexConsumerExtended vce = ((VertexConsumerExtended) vc);
            vce.canpipe_recomputeNormals(true);

            if (
                bs != null &&
                vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                MaterialMap materialMap = MaterialMaps.getForBlock(bs.getBlock());
                vce.canpipe_setSharedMaterialMap(materialMap);
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
                "[FFFFF[II"+
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
            VertexConsumerExtended vce = ((VertexConsumerExtended) vc);
            vce.canpipe_recomputeNormals(false);

            if (
                bs != null &&
                vce.canpipe_getVertexFormat().contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
            ) {
                vce.canpipe_setSharedMaterialMap(null);
            }
        }
    }*/

}
