package fewizz.canpipe.renderpearl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.SpvModule;

public interface PipelineBuilderExtended {

    CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        @Nullable Consumer<SpvModule.Reflection.Descriptor> onUnhandledUniform
    );

}
