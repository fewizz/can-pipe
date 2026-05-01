package fewizz.canpipe.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {

    @Accessor("outputTarget") OutputTarget canpipe_getOutputTarget();

    @Accessor("textures") Map<String, RenderSetup.TextureBinding> canpipe_getTextures();

    @Accessor("pipeline") RenderPipeline canpipe_getPipeline();

}
