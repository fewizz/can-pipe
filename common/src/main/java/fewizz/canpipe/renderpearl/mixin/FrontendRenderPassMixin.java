package fewizz.canpipe.renderpearl.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;

@Mixin(FrontendRenderPass.class)
public class FrontendRenderPassMixin {

    // TODO
    @Shadow @Final private List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments;

    @ModifyArg(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V",
            ordinal = 0
        ),
        index = 0
    )
    String moreDetailedMismatchedAttachmentCountError(
        String message,
        @Local List<ColorTargetState> colorTargetStates,
        @Local CompiledRenderPipeline pipeline
    ) {
        FrontendRenderPipeline frontendRenderPipeline = (FrontendRenderPipeline) pipeline;
        return message+" Expected "+colorTargetStates.size()+" color attachments, but got "+this.colorAttachments.size()+" for pipeline \""+frontendRenderPipeline.name()+"\"";
    }

    @ModifyArg(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V",
            ordinal = 1
        ),
        index = 0
    )
    String moreDetailedFormatMismatchError(
        String message,
        @Local ColorTargetState colorTargetState,
        @Local RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment,
        @Local CompiledRenderPipeline pipeline
    ) {
        String pipelineName = ((FrontendRenderPipeline) pipeline).name();
        String textureName = attachment.textureView().texture().getLabel();
        GpuFormat gotFormat = attachment.textureView().texture().getFormat();

        return message+" Expected format \""+colorTargetState.format()+"\", but got \""+gotFormat+"\"."
            +" Texture: \""+textureName+"\", Render pipeline: \""+pipelineName+"\".";
    }

}
