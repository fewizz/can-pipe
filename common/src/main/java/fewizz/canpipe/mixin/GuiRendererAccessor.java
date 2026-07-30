package fewizz.canpipe.mixin;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiRenderer.class)
public interface GuiRendererAccessor {

    @Invoker("invalidateItemAtlas")
    void canpipe_invalidateItemAtlas();

}
