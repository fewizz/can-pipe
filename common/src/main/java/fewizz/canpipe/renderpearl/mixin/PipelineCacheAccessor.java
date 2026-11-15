package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.ShaderSource;

@Mixin(PipelineCache.class)
public interface PipelineCacheAccessor {

    @Accessor("shaderSource")
    ShaderSource canpipe_getShaderSource();

}
