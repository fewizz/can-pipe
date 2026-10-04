package fewizz.canpipe.renderpearl;

import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;

public interface PipelineCacheExtended {

    @Nullable RenderPipeline canpipe_getPipeline(CompiledRenderPipeline id);

}
