package fewizz.canpipe.mixininterface;

import java.util.function.Function;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.pipeline.Pipeline;

public interface CompositeRenderTypeExtended {

    void canpipe_setMaterialRenderPipelineCreationFunction(Function<Pipeline, RenderPipeline> pipeline);
    void canpipe_setMaterialShadowRenderPipelineCreationFunction(Function<Pipeline, RenderPipeline> pipeline);
    RenderPipeline canpipe_getRenderPipeline();

}
