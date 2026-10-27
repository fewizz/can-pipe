package fewizz.canpipe.renderpearl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.SpvModule;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

public interface PipelineBuilderExtended {

    CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        @Nullable Consumer<SpvModule.Reflection.Descriptor> onUnhandledUniform,
        boolean autoMapLocations  // added
    );

    BackendRenderPipeline.CreateInfo canpipe_generateBackendCreateInfo(
        RenderPipeline pipeline, ShaderSource shaderSource,
        ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
        Object2IntOpenHashMap<String> uniformBindings,
        boolean autoMapLocations  // added
    );

}
