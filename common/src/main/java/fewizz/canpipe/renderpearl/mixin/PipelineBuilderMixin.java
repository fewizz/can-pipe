package fewizz.canpipe.renderpearl.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;

import fewizz.canpipe.renderpearl.PipelineBuilderExtended;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin implements PipelineBuilderExtended {

    @Unique private ThreadLocal<Consumer<SpvModule.Reflection.Descriptor>> canpipe_onUnhandledUniform = new ThreadLocal<>();

    @Shadow abstract CompletableFuture<CompiledRenderPipeline.Pending> compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource, Executor executor);

    @Override
    public CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        @Nullable Consumer<SpvModule.Reflection.Descriptor> onUnhandledUniform
    ) {
        try {
            this.canpipe_onUnhandledUniform.set(onUnhandledUniform);
            return this.compilePipeline(pipeline, shaderSource, executor);
        }
        finally {
            this.canpipe_onUnhandledUniform.remove();
        }
    }

    @ModifyExpressionValue (
        method = "generateBackendCreateInfo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/backend/api/SpvModule$Reflection;descriptors()Ljava/util/List;"
        )
    )
    List<SpvModule.Reflection.Descriptor> handleUndefinedUniform(
        List<SpvModule.Reflection.Descriptor> result,
        @Local Map<String, BindGroupLayout.UniformDescription> pipelineUniformsByName
    ) {
        var handler = this.canpipe_onUnhandledUniform.get();
        if (handler != null) {
            result = result.stream().filter(d -> {
                if (!pipelineUniformsByName.containsKey(d.name())) {
                    handler.accept(d);
                    return false;
                }
                return true;
            }).toList();
        }
        return result;
    }

}
