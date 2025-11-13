package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.api.hg.HgGraphicsPipeline;
import graphics.cinnabar.core.mercury.MercuryGraphicsPipeline;

@Mixin(MercuryGraphicsPipeline.class)
public class MercuryGraphicsPipelineMixin {

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgRenderPass;colorAttachmentCount()I"
        )
    )
    int clampBlendAttachmentCount(int count, @Local HgGraphicsPipeline.Blend blend) {
        if (blend != null) {
            count = Math.min(count, blend.attachments().size());
        }
        return count;
    }

}
