package fewizz.canpipe.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @WrapOperation(
        method = {"renderSolid", "renderTranslucent"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem("+
                "Lnet/minecraft/world/item/ItemDisplayContext;"+
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+
                "Lnet/minecraft/client/renderer/MultiBufferSource;"+
                "II[I"+
                "Ljava/util/List;"+
                "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"+
            ")V"
        )
    )
    void setItemStack(
        final ItemDisplayContext type,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int lightCoords,
        int overlayCoords,
        int[] tintLayers,
        List<BakedQuad> quads,
        ItemStackRenderState.FoilType foilType,
        Operation<Void> operation,
        @Local SubmitNodeStorage.ItemSubmit submit,
        @Local(argsOnly = true) SubmitNodeCollection nodeCollection
    ) {
        ItemSubmitExtra extra = ((SubmitNodeCollectorExtended) nodeCollection).canpipe_getItemSubmitExtras().get(submit);
        try {
            ItemRendererAccessor.canpipe_setItemStack(extra.itemStack());
            operation.call(type, poseStack, bufferSource, lightCoords, overlayCoords, tintLayers, quads, foilType);
        } finally {
            ItemRendererAccessor.canpipe_setItemStack(null);
        }
    }

}
