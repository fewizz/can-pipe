package fewizz.canpipe.mixin;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.pipeline.Pipeline;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(value = ItemRenderer.class, priority = 1000)
public class ItemRendererMixin {

    @Unique private static ItemStack canpipe_itemStack = null;

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"
        )
    )
    private static void beforeRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(argsOnly = true, ordinal = 0) LocalRef<ItemStackRenderState.FoilType> foilType,
        @Local(argsOnly = true) MultiBufferSource bufferSource
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
        Item item = canpipe_itemStack.getItem();

        if (item instanceof BlockItem bi) {
            MaterialMap materialMap = MaterialMaps.getForBlock(bi.getBlock());
            vce.canpipe_setScopedMaterialMap(materialMap);
        }
        else {
            MaterialMap materialMap = MaterialMaps.getForItem(item);
            vce.canpipe_setScopedMaterialMap(materialMap);
        }

        if (foilType.get() != ItemStackRenderState.FoilType.NONE) {
            vce.canpipe_setScopedGlint(true);
            foilType.set(ItemStackRenderState.FoilType.NONE);
        }
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value="INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad("+
                "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"+
                "Lnet/minecraft/client/resources/model/geometry/BakedQuad;"+
                "Lcom/mojang/blaze3d/vertex/QuadInstance;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    private static void afterRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(argsOnly = true) MultiBufferSource bufferSource
    ) {
        VertexConsumerExtended vce = (VertexConsumerExtended) bufferSource.getBuffer(renderType);
        vce.canpipe_setScopedGlint(false);
        vce.canpipe_setScopedMaterialMap(null);
    }

}
