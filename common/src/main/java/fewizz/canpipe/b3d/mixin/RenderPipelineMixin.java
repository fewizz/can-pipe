package fewizz.canpipe.b3d.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineExtended;

// Ugly
@Mixin(RenderPipeline.class)
public class RenderPipelineMixin implements RenderPipelineExtended {

    private Set<String> canpipe_optionalSamplers = null;

    @Override public Set<String> canpipe_getOptionalSamplers() { return canpipe_optionalSamplers; }

}