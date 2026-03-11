package fewizz.canpipe.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {

    @Accessor(value = "outputTarget")
    OutputTarget canpipe_getOutputTarget();

    @Accessor(value = "textures")
    Map<String, RenderSetup.TextureBinding> canpipe_getTextures();

}
