package fewizz.canpipe.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.mixininterface.ItemStackRenderStateAccessor;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @Inject(
        method = "updateForTopItem",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;isLeftHand:Z")
    )
    void onBeforeAppendLayer(
        ItemStackRenderState itemStackRenderState,
        ItemStack itemStack,
        ItemDisplayContext itemDisplayContext,
        boolean bl,
        @Nullable Level level,
        @Nullable LivingEntity livingEntity,
        int i,
        CallbackInfo ci
    ) {
        ((ItemStackRenderStateAccessor) itemStackRenderState).setItem(itemStack.getItem());
    }

}
