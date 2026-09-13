package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

@Mixin(PipelineBuilder.class)
public interface PipelineBuilderAccessor {

    @Invoker("generateBackendCreateInfo")
    BackendRenderPipeline.CreateInfo canpipe_invokeGenerateBackendCreateInfo(
        RenderPipeline pipeline,
		ShaderSource shaderSource,
		ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos,
		Object2IntOpenHashMap<String> uniformBindings
    );

}
