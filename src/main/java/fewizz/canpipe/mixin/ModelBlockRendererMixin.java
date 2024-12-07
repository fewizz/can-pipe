package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipeVertexFormatElements;
import fewizz.canpipe.MaterialMap;
import fewizz.canpipe.MaterialMaps;
import fewizz.canpipe.Materials;
import fewizz.canpipe.Pipelines;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {

    // Shading is controlled by pipeline
    @ModifyExpressionValue(
        method = {"renderModelFaceAO", "renderModelFaceFlat"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/model/BakedQuad;isShade()Z"
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
        BlockAndTintGetter blockAndTintGetter,
        BlockState bs,
        BlockPos bp,
        VertexConsumer vc,
        PoseStack.Pose pose,
        BakedQuad bakedQuad,
        float r, float g, float b, float a, int ao0, int ao1, int ao2, int ao3, int overlay,
        CallbackInfo ci
    ) {
        if (vc instanceof BufferBuilder bb && bb.format.contains(CanPipeVertexFormatElements.MATERIAL_INDEX)) {
            int index = -1;
            if (bs != null) {
                ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(bs.getBlock());
                MaterialMap materialMap = MaterialMaps.BLOCKS.get(rl);
                if (materialMap != null && materialMap.defaultMaterial != null) {
                    index = Materials.id(materialMap.defaultMaterial);
                }
            }
            ((VertexConsumerExtended) bb).setSharedMaterialIndex(index);
            return;
        }
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD")
    )
    void onBeforeSingleBlockData(
        PoseStack.Pose pose,
        VertexConsumer vc,
        @Nullable BlockState bs,
        BakedModel bakedModel,
        float r, float g, float b, int light, int overlay,
        CallbackInfo ci
    ) {
        if (vc instanceof BufferBuilder bb && bb.format.contains(CanPipeVertexFormatElements.MATERIAL_INDEX)) {
            int index = -1;
            if (bs != null) {
                ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(bs.getBlock());
                MaterialMap materialMap = MaterialMaps.BLOCKS.get(rl);
                if (materialMap != null && materialMap.defaultMaterial != null) {
                    index = Materials.id(materialMap.defaultMaterial);
                }
            }
            ((VertexConsumerExtended) bb).setSharedMaterialIndex(index);
            return;
        }
    }

}
