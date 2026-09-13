package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.backend.opengl.GlRenderPass;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;

@Mixin(GlRenderPass.class)
public interface GlRenderPassAccessor {

    @Accessor("pipeline")
    public GlRenderPipeline canpipe_getPipeline();

}
