package fewizz.canpipe.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fewizz.canpipe.mixininterface.ItemStackLayerRenderStateExtended;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CuboidItemModelWrapper.class)
public class CuboidItemModelWrapperMixin {

    @Inject(
        method = "update",
        at = @At("RETURN")
    )
    void onUpdate(CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer, @Local(argsOnly = true) ItemStack itemStack) {
        ((ItemStackLayerRenderStateExtended) layer).canpipe_setItemStack(itemStack);
    }

}
