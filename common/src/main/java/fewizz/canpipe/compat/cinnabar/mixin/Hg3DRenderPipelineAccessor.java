package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import graphics.cinnabar.api.hg.HgGraphicsPipeline;
import graphics.cinnabar.core.hg3d.Hg3DRenderPipeline;

@Mixin(Hg3DRenderPipeline.class)
public interface Hg3DRenderPipelineAccessor {

    @Accessor("shaderSourceCache")
    static Map</*Hg3DRenderPipeline.ShaderSourceCacheKey*/ Object, String> getShaderSourceCache() {
        return null;
    }

    @Accessor("shaderSet")
    HgGraphicsPipeline.ShaderSet get_shaderSet();

}
