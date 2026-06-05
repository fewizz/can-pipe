package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.resources.model.sprite.SpriteId;

@Mixin(OrderedSubmitNodeCollector.class)
public interface OrderedSubmitNodeCollectorMixin {

    @Inject(
        method = "submitModel("+
            "Lnet/minecraft/client/model/Model;"+
            "Ljava/lang/Object;"+
            "Lcom/mojang/blaze3d/vertex/PoseStack;"+
            "IIILnet/minecraft/client/resources/model/sprite/SpriteId;"+
            "Lnet/minecraft/client/resources/model/sprite/SpriteGetter;"+
            "ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;"+
        ")V",
        at = @At("HEAD")
    )
    default void onSubmitModelWithSpriteId(CallbackInfo ci, @Local(argsOnly = true) SpriteId sprite) {
        if (this instanceof SubmitNodeCollectorExtended snce) {
            snce.canpipe_setPendingSpriteID(sprite);
        }
    }

}
