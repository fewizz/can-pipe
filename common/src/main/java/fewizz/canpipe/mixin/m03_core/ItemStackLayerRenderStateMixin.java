package fewizz.canpipe.mixin.m03_core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.helpers.WrappingMultiBufferSourceThatSetsItemMaterialIndex;
import fewizz.canpipe.mixininterface.ItemStackLayerRenderStateExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemStackLayerRenderStateMixin implements ItemStackLayerRenderStateExtended {

    @Unique ItemStack canpipe_itemStack;

    @Override
    public void setItemStack(ItemStack item) {
        this.canpipe_itemStack = item;
    }

    @Override
    public ItemStack getItemStack() {
        return this.canpipe_itemStack;
    }

    @Inject(method = "clear", at = @At("TAIL"))
    void onAfterClear(CallbackInfo ci) {
        this.canpipe_itemStack = null;
    }

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem("+
                "Lnet/minecraft/world/item/ItemDisplayContext;"+  // 0
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+  // 1
                "Lnet/minecraft/client/renderer/MultiBufferSource;"+  // 2
                "II[I"+
                "Ljava/util/List;"+
                "Lnet/minecraft/client/renderer/RenderType;"+
                "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"+
            ")V"
        ),
        index = 2
    )
    MultiBufferSource replaceBufferSourceOnRender(MultiBufferSource source) {
        return new WrappingMultiBufferSourceThatSetsItemMaterialIndex(
            source,
            this.getItemStack().getItem()
        );
    }

}
