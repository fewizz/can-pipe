package fewizz.canpipe.renderpearl.mixin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;

import fewizz.canpipe.renderpearl.GlslCompilerExtended;
import fewizz.canpipe.renderpearl.PipelineBuilderExtended;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin implements PipelineBuilderExtended {

    @Shadow @Final private GlslCompiler compiler;

    @Shadow abstract CompletableFuture<CompiledRenderPipeline.Pending> compilePipeline(RenderPipeline pipeline, ShaderSource shaderSource, Executor executor);
    @Shadow private BackendRenderPipeline.CreateInfo generateBackendCreateInfo(RenderPipeline pipeline, ShaderSource shaderSource, ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos, Object2IntOpenHashMap<String> uniformBindings) { return null; }

    @Unique private ThreadLocal<Consumer<SpvModule.Reflection.Descriptor>> canpipe_onUnhandledUniform = new ThreadLocal<>();
    @Unique private ThreadLocal<Boolean> canpipe_autoMapLocations = new ThreadLocal<>();

    @Override
    public CompletableFuture<CompiledRenderPipeline.Pending> canpipe_compilePipeline(
        RenderPipeline pipeline, ShaderSource shaderSource, Executor executor,
        @Nullable Consumer<SpvModule.Reflection.Descriptor> onUnhandledUniform,
        boolean autoMapLocations
    ) {
        try {
            this.canpipe_onUnhandledUniform.set(onUnhandledUniform);
            this.canpipe_autoMapLocations.set(autoMapLocations);
            return this.compilePipeline(pipeline, shaderSource, executor);
        }
        finally {
            this.canpipe_onUnhandledUniform.remove();
            this.canpipe_autoMapLocations.remove();
        }
    }

    @Override
    public BackendRenderPipeline.CreateInfo canpipe_generateBackendCreateInfo(
        RenderPipeline pipeline, ShaderSource shaderSource,
        ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
        Object2IntOpenHashMap<String> uniformBindings,
        boolean autoMapLocations  // added
    ) {
        try {
            this.canpipe_autoMapLocations.set(autoMapLocations);
            return this.generateBackendCreateInfo(pipeline, shaderSource, shaderCreateInfos, uniformBindings);
        }
        finally {
            this.canpipe_autoMapLocations.remove();
        }
    }

    @ModifyExpressionValue(
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

    @Inject(
        method = "generateBackendCreateInfo",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/frontend/shaders/GlslCompiler;compileToSpv"
        )
    )
    void extendedCompileToSpv(CallbackInfoReturnable<BackendRenderPipeline.CreateInfo> ci) {
        Boolean autoMapLocations = this.canpipe_autoMapLocations.get();
        if (autoMapLocations != null && autoMapLocations) {
            ((GlslCompilerExtended) this.compiler).canpipe_enableAutoMapLocationsForPendingCompilation();
        }
    }

    @ModifyArg(
        method = "compilePipeline",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/concurrent/CompletableFuture;supplyAsync"
        ),
        index = 0
    )
    Supplier<CompiledRenderPipeline.Pending> onSupplyAsync(Supplier<CompiledRenderPipeline.Pending> supplier) {
        Boolean autoMapLocations = this.canpipe_autoMapLocations.get();
        if (autoMapLocations != null && autoMapLocations) {
            return () -> {  // Wrap, enable autoMapLocations
                this.canpipe_autoMapLocations.set(true);
                try {
                    return supplier.get();
                }
                finally {
                    this.canpipe_autoMapLocations.remove();
                }
            };
        }
        return supplier;
    }

}
