package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;

public class MaterialProgramLoader {

    RenderPipeline.Builder pipelineBuilder;
    RenderPipeline originalRenderPipeline;
    Map<Pair<List<GpuFormat>, GpuFormat>, RenderPipeline> renderPipelinesPerFormat;

    public RenderPipeline getOrCompileRenderPipeline(Pair<List<GpuFormat>, GpuFormat> formats) {
        RenderPipeline result = this.renderPipelinesPerFormat.get(formats);

        if (result == null) {
            ((RenderPipelineBuilderExtended) this.pipelineBuilder).canpipe_resetActiveColorTargetStateCount();
            var colorFormats = formats.getLeft();
            // var deothFormat = formats.getRight();

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

            result = pipelineBuilder.build();

            this.renderPipelinesPerFormat.put(formats, result);
        }

        return result;
    }

}
