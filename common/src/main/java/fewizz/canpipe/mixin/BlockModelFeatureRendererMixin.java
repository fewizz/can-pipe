package fewizz.canpipe.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.WrappedListWithExtraElement;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;

@Mixin(BlockModelFeatureRenderer.class)
public class BlockModelFeatureRendererMixin {

    @ModifyExpressionValue(
        method = "buildGroup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/BlockModelFeatureRenderer$Submit;modelParts()Ljava/util/List;"
        )
    )
    List<BlockStateModelPart> setMaterial(List<BlockStateModelPart> modelParts, @Local(name = "wrappedBuffer") VertexConsumer wrappedBuffer) {
        if (wrappedBuffer instanceof VertexConsumerExtended vce && modelParts instanceof WrappedListWithExtraElement cl) {
            MaterialMap materialMap = (MaterialMap) cl.element;
            vce.canpipe_setScopedMaterialSupplier(sprite -> materialMap.getMaterial(sprite));
        }
        return modelParts;
    }

}
