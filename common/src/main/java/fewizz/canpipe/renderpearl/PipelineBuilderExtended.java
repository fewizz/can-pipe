package fewizz.canpipe.renderpearl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

public interface PipelineBuilderExtended {

    CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        ExtraPipelineBuilderOptions extraOptions  // added
    );

    BackendRenderPipeline.CreateInfo canpipe_generateBackendCreateInfo(
        RenderPipeline pipeline, ShaderSource shaderSource,
        ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
        Object2IntOpenHashMap<String> uniformBindings,
        ExtraPipelineBuilderOptions extraOptions  // added
    );

}
