package fewizz.canpipe.mixin.settings;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.components.AbstractSelectionList;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {

    @Invoker("addEntry")
    int callAddEntry(AbstractSelectionList.Entry<?> e);

}
