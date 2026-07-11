package fewizz.canpipe.b3d.mixin;

import java.util.Optional;

import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassDescriptor;

@Mixin(RenderPass.class)
public class RenderPassMixin {

    @ModifyArg(
        method = "setPipeline",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"
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
