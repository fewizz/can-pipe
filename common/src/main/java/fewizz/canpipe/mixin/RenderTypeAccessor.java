package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(RenderType.class)
public interface RenderTypeAccessor {

    @Accessor(value = "name", remap = false)
    String canpipe_getName();

    @Accessor(value = "state", remap = false)
    RenderSetup canpipe_getState();

}
