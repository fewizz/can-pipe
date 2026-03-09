package fewizz.canpipe.mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemRenderer.class, priority = 1001)
public interface ItemRendererAccessor {

    @Accessor("canpipe_itemStack")
    public static void canpipe_setItemStack(ItemStack itemStack) {}

}
