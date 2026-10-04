package fewizz.canpipe.renderpearl.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(RenderSystem.class)
public interface RenderSystemAccessor {

    @Accessor("currentPipelineCache")
    public static @Nullable PipelineCache canpipe_getCurrentPipelineCache() { return null; }

}
