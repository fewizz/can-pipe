package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @Unique private ItemStack canpipe_itemStack = null;

    @WrapOperation(
        method = {"renderSolid", "renderTranslucent"},
        require = 2,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;renderItem("+
                "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"+
                "Lnet/minecraft/client/renderer/OutlineBufferSource;"+
                "Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;"+
            ")V"
        )
    )
    void setItemStack(
        ItemFeatureRenderer instance,
        BufferSource bufferSource,
        OutlineBufferSource outlineBufferSource,
        SubmitNodeStorage.ItemSubmit submit,
        Operation<Void> operation,
        @Local SubmitNodeCollection nodeCollection
    ) {
        ItemSubmitExtra extra = ((SubmitNodeCollectorExtended) nodeCollection).canpipe_getItemSubmitExtras().get(submit);
        try {
            this.canpipe_itemStack = extra.itemStack();
            operation.call(instance, bufferSource, outlineBufferSource, submit);
        } finally {
            this.canpipe_itemStack = null;
        }
    }

    @Inject(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"
        )
    )
    private void beforeRenderItem(
        CallbackInfo ci,
        @Local RenderType renderType,
        @Local(ordinal = 0) LocalRef<ItemStackRenderState.FoilType> foilType,
        @Local(argsOnly = true) BufferSource buffer
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        VertexConsumer vc = buffer.getBuffer(renderType);
        VertexConsumerExtended vce = (VertexConsumerExtended) vc;
        Item item = this.canpipe_itemStack.getItem();

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
        @Local(argsOnly = true) BufferSource buffer
    ) {
        VertexConsumer vc = buffer.getBuffer(renderType);
        VertexConsumerExtended vce = (VertexConsumerExtended) vc;
        vce.canpipe_setScopedGlint(false);
        vce.canpipe_setScopedMaterialMap(null);
    }

}
