package fewizz.canpipe.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;

import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;

@Mixin(RenderPass.class)
public class RenderPassMixin {

    @Shadow @Final private List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments;

    @ModifyVariable(
        method = "setPipeline",
        at = @At("HEAD")
    )
    RenderPipeline onSetPipeline(RenderPipeline pipeline) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            pipeline = p.onRenderPassSetPipeline(pipeline, this.colorAttachments);
        }
        return pipeline;
    }

}
