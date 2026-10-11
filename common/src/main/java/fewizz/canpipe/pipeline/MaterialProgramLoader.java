package fewizz.canpipe.pipeline;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;

import fewizz.canpipe.mixin.RenderPipelineBuilderAccessor;
import fewizz.canpipe.renderpearl.RenderPipelineBuilderExtended;
import fewizz.canpipe.renderpearl.mixin.FrontendGpuDeviceAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceLists;
import net.minecraft.resources.Identifier;

public class MaterialProgramLoader implements Closeable {

    @SuppressWarnings("unused") private final Identifier id;
    private final RenderPipeline.Builder pipelineBuilder;
    private final RenderPipeline originalRenderPipeline;
    private final Map<Pair<List<GpuFormat>, GpuFormat>, CompiledRenderPipeline> renderPipelinesPerFormat = new HashMap<>();

    private final BackendRenderPipeline.CreateInfo createInfo;
    private final ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos;
    private final Object2IntOpenHashMap<String> uniformBindings;

    MaterialProgramLoader(
        Identifier id, RenderPipeline.Builder pipelineBuilder, RenderPipeline originalRenderPipeline,
        BackendRenderPipeline.CreateInfo createInfo, ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos, Object2IntOpenHashMap<String> uniformBindings
    ) {
        this.id = id;
        this.pipelineBuilder = pipelineBuilder;
        this.originalRenderPipeline = originalRenderPipeline;

        this.createInfo = createInfo;
        this.shaderCreateInfos = shaderCreateInfos;
        this.uniformBindings = uniformBindings;
    }

    public RenderPipeline createRenderPipeline(Pair<List<GpuFormat>, GpuFormat> formats) {
        ((RenderPipelineBuilderExtended) this.pipelineBuilder).canpipe_resetActiveColorTargetStateCount();
        var colorFormats = formats.getLeft();

        var originalBlendFunction = this.originalRenderPipeline.getColorTargetStates().get(0).blendFunction();

        for (int i = 0; i < colorFormats.size(); ++i) {
            var format = colorFormats.get(i);
            if (format != null) {
                boolean blend = !(
                    format.componentType() == GpuFormat.ComponentType.SINT_8 ||
                    format.componentType() == GpuFormat.ComponentType.UINT_8 ||
                    format.componentType() == GpuFormat.ComponentType.SINT_16 ||
                    format.componentType() == GpuFormat.ComponentType.UINT_16 ||
                    format.componentType() == GpuFormat.ComponentType.SINT_32 ||
                    format.componentType() == GpuFormat.ComponentType.UINT_32
                );
                pipelineBuilder.withColorTargetState(
                    i,
                    new ColorTargetState(blend ? originalBlendFunction : Optional.empty(), format, ColorTargetState.WRITE_ALL)
                );
            }
            else {
                pipelineBuilder.withUnusedColorTargetState(i);
            }
        }

        return pipelineBuilder.build();
    }

    public CompiledRenderPipeline getOrCreateCompiledRenderPipeline(Pair<List<GpuFormat>, GpuFormat> formats) {
        return this.renderPipelinesPerFormat.computeIfAbsent(formats, (_formats) -> {
            var pipeline = this.createRenderPipeline(formats);

            var deviceBackend = ((FrontendGpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();

            var pending = deviceBackend.compilePipeline(new BackendRenderPipeline.CreateInfo(
                this.createInfo.name(),
                this.createInfo.shaders(),
                this.createInfo.vertexBuffers(),
                this.createInfo.attribBindings(),
                this.createInfo.uniforms(),
                this.createInfo.pushConstantsSize(),
                this.createInfo.depthStencilState(),
                this.createInfo.polygonMode(),
                this.createInfo.cull(),
                pipeline.getColorTargetStates(),
                this.createInfo.primitiveTopology()
            ));

            var backendPipeline = pending.finishCompile();
            return new FrontendRenderPipeline(
                pipeline.getLocation().toString(),
                backendPipeline,
                ReferenceLists.unmodifiable(new ReferenceArrayList<>(pipeline.getVertexFormatBindings())),
                Object2IntMaps.unmodifiable(uniformBindings),
                BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()),
                ReferenceLists.unmodifiable(new ReferenceArrayList<>(pipeline.getColorTargetStates())),
                pipeline.wantsDepthTexture(),
                pipeline.pushConstantSize()
            );
        });
    }

    public VertexFormat vertexFormat() {
        return ((RenderPipelineBuilderAccessor) this.pipelineBuilder).canpipe_getVertexFormatPerBuffer()[0];
    }

    @Override
    public void close() {
        this.shaderCreateInfos.forEach(shaderModule -> shaderModule.module().close());
        this.renderPipelinesPerFormat.values().forEach(CompiledRenderPipeline::close);
    }

}
