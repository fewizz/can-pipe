package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.ItemStackLayerRenderStateExtended;
import fewizz.canpipe.mixininterface.SubmitNodeCollectorExtended;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemStackLayerRenderState implements ItemStackLayerRenderStateExtended {

    @Unique ItemStack canpipe_itemStack = null;

    @Override
    public void canpipe_setItemStack(ItemStack itemStack) {
        this.canpipe_itemStack = itemStack;
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
        Item item = this.canpipe_itemStack.getItem();

        if (submitNodeCollector instanceof SubmitNodeCollectorExtended snce) {
            MaterialMap materialMap;
            if (item instanceof BlockItem bi) {
                materialMap = MaterialMaps.getForBlock(bi.getBlock());
            }
            else {
                materialMap = MaterialMaps.getForItem(item);
            }
            snce.canpipe_setPendingItemSubmitMaterialMap(materialMap);
        }
    }

}
