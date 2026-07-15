package fewizz.canpipe.b3d.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;

@Mixin(RenderPass.class)
public class RenderPassMixin {

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
        @Local ColorTargetState[] colorTargetStates
    ) {
        return message+" Expected "+colorTargetStates.length+" color attachments, but got "+this.colorAttachments.size()+".";
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
        @Local RenderPassDescriptor.Attachment<Optional<Vector4fc>> attachment
    ) {
        return message+" Expected format "+colorTargetState.format()+", but got "+attachment.textureView().texture().getFormat()+".";
    }

}
