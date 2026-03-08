package fewizz.canpipe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockFeatureRenderer.class)
public class BlockFeatureRendererMixin {

/*    @Inject(
        method = "renderBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/BlockFeatureRenderer;putPartQuads("+
                "Lnet/minecraft/client/renderer/block/model/BlockModelPart;"+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
                "[I"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
            ")V"
        )
    )
    void postSetTerrainBlockMaterial(
        CallbackInfo ci,
        @Local(ordinal = 0) SubmitNodeStorage.BlockModelSubmit submit,
        @Local(ordinal = 0) VertexConsumer vertexConsumer
    ) {
        BlockState block = ((MultiPartModelAccessor) submit.model()).canpipe_getBlockState();
        MaterialMap materialMap = MaterialMaps.getForBlock(block.getBlock());
        ((VertexConsumerExtended) vertexConsumer).canpipe_setSharedMaterialMap(materialMap);
    }

    @Inject(
        method = "renderBlockModelSubmits",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/BlockFeatureRenderer;putPartQuads("+
                "Lnet/minecraft/client/renderer/block/model/BlockModelPart;"+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
                "[I"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
            ")V"
        )
    )
    void preSetTerrainBlockMaterial(
        CallbackInfo ci,
        @Local(ordinal = 0) VertexConsumer vertexConsumer
    ) {
        ((VertexConsumerExtended) vertexConsumer).canpipe_setSharedMaterialMap(null);
    }*/

}
