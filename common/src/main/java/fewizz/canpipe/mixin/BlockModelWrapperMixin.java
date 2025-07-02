package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.mixininterface.ItemStackLayerRenderStateExtended;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

@Mixin(BlockModelWrapper.class)
public class BlockModelWrapperMixin {

    @Inject(method = "update", at = @At("TAIL"))
    void onUpdateEnd(
        CallbackInfo ci,
        @Local ItemStackRenderState.LayerRenderState layer,
        @Local(argsOnly = true) ItemStack itemStack
    ) {
        ((ItemStackLayerRenderStateExtended) layer).setItemStack(itemStack);
    }

}
