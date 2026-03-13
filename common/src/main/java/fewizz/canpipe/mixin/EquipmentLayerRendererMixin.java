package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.world.item.ItemStack;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    @ModifyExpressionValue(
        method = "renderLayers("+
            "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"+
            "Lnet/minecraft/resources/ResourceKey;"+
            "Lnet/minecraft/client/model/Model;"+
            "Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"+
            "Lcom/mojang/blaze3d/vertex/PoseStack;"+
            "Lnet/minecraft/client/renderer/SubmitNodeCollector;"+
            "I"+
            "Lnet/minecraft/resources/Identifier;"+
            "II"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;order(I)Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;",
            ordinal = 0
        )
    )
    OrderedSubmitNodeCollector onRenderLayer(
        OrderedSubmitNodeCollector collector,
        @Local ItemStack itemStack
    ) {
        if (itemStack.hasFoil()) {
            ((SubmitNodeCollectorExtended) collector).canpipe_setPendingModelEntityGlint();
        }
        return collector;
    }

    @ModifyExpressionValue(
        method = "renderLayers("+
            "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"+
            "Lnet/minecraft/resources/ResourceKey;"+
            "Lnet/minecraft/client/model/Model;"+
            "Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"+
            "Lcom/mojang/blaze3d/vertex/PoseStack;"+
            "Lnet/minecraft/client/renderer/SubmitNodeCollector;"+
            "I"+
            "Lnet/minecraft/resources/Identifier;"+
            "II"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hasFoil()Z",
            ordinal = 0
        )
    )
    boolean onGetItemFoil(boolean hasFoil) {
        if (Pipelines.getCurrent() != null) {
            hasFoil = false;
        }
        return hasFoil;
    }

}
