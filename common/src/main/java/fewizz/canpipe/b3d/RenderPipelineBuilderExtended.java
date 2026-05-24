package fewizz.canpipe.b3d;

import com.mojang.blaze3d.pipeline.RenderPipeline;

public interface RenderPipelineBuilderExtended {

    /* There won't be a warning if pipeline doesn't use this variable */
    RenderPipeline.Builder canpipe_withOptionalSampler(String sampler);

}
