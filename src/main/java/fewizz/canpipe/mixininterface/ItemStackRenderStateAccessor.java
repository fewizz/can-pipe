package fewizz.canpipe.mixininterface;

import net.minecraft.world.item.Item;

public interface ItemStackRenderStateAccessor {

    void setItem(Item item);
    Item getItem();

}
