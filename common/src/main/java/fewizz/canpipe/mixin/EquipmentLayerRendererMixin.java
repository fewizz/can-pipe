package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {

    /*@Inject(
        method = "renderLayers("+
            "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"+
            "Lnet/minecraft/resources/ResourceKey;"+
            "Lnet/minecraft/client/model/Model;"+
            "Lnet/minecraft/world/item/ItemStack;"+
            "Lcom/mojang/blaze3d/vertex/PoseStack;"+
            "Lnet/minecraft/client/renderer/MultiBufferSource;"+
            "I"+
            "Lnet/minecraft/resources/ResourceLocation;"+
        ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/Model;renderToBuffer("+
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+
                "Lcom/mojang/blaze3d/vertex/VertexConsumer;"+
                "III"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void onRenderLayers(
        CallbackInfo ci,
        @Local VertexConsumer vertexConsumer
    ) {
        if (vertexConsumer instanceof VertexConsumerExtended vce) {
            vce.canpipe_setSharedGlint(false);
        }
    }*/

}
