package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import fewizz.canpipe.mixininterface.ItemStackRenderStateExtended;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.Item;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements ItemStackRenderStateExtended {

    @Unique
    Item item;

    @Override
    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Item getItem() {
        return this.item;
    }

}
