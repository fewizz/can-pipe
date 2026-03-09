package fewizz.canpipe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fewizz.canpipe.helpers.ItemSubmitExtra;
import fewizz.canpipe.mixininterface.ItemStackLayerRenderStateExtended;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemStackLayerRenderState implements ItemStackLayerRenderStateExtended {

    @Unique ItemStack canpipe_itemStack = null;

    @Override
    public void canpipe_setItemStack(ItemStack itemStack) {
        this.canpipe_itemStack = itemStack;
    }

    @Override
    public ItemStack canpipe_getItemStack() {
        return this.canpipe_itemStack;
    }

    @Inject(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem("+
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+
                "Lnet/minecraft/world/item/ItemDisplayContext;"+
                "III[I"+
                "Ljava/util/List;"+
                "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"+
            ")V"
        )
    )
    void onSubmit(CallbackInfo ci, @Local(argsOnly = true) SubmitNodeCollector submitNodeCollector) {
        if (submitNodeCollector instanceof SubmitNodeCollectorExtended snce) {
            snce.canpipe_setPendingItemSubmitExtra(new ItemSubmitExtra(this.canpipe_itemStack));
        }
    }

}
