package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;

@Mixin(GlRenderPipeline.class)
public interface GlRenderPipelineAccessor {

}
