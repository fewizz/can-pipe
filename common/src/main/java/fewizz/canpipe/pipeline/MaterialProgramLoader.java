package fewizz.canpipe.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;

public class MaterialProgramLoader {

    private final RenderPipeline.Builder pipelineBuilder;
    private final RenderPipeline originalRenderPipeline;
    private final Map<Pair<List<GpuFormat>, GpuFormat>, RenderPipeline> renderPipelinesPerFormat = new HashMap<>();

    MaterialProgramLoader(RenderPipeline.Builder pipelineBuilder, RenderPipeline originalRenderPipeline) {
        this.pipelineBuilder = pipelineBuilder;
        this.originalRenderPipeline = originalRenderPipeline;
    }

    public RenderPipeline getOrCompileRenderPipeline(Pair<List<GpuFormat>, GpuFormat> formats) {
        return this.renderPipelinesPerFormat.computeIfAbsent(formats, (_formats) -> {
            ((RenderPipelineBuilderExtended) this.pipelineBuilder).canpipe_resetActiveColorTargetStateCount();
            var colorFormats = formats.getLeft();
            // var depthFormat = formats.getRight();

            var originalBlendFunction = this.originalRenderPipeline.getColorTargetState().blendFunction();
            var originalDSState = originalRenderPipeline.getDepthStencilState();

            for (int i = 0; i < colorFormats.size(); ++i) {
                pipelineBuilder.withColorTargetState(i, new ColorTargetState(originalBlendFunction, colorFormats.get(i), ColorTargetState.WRITE_ALL));
            }
            pipelineBuilder.withDepthStencilState(new DepthStencilState(
                originalDSState.depthTest(),
                originalDSState.writeDepth(),
                originalDSState.depthBiasScaleFactor(),
                originalDSState.depthBiasConstant()
            ));

            return pipelineBuilder.build();
        });
    }

}
