package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.core.mercury.MercuryFramebuffer;

@Mixin(MercuryFramebuffer.class)
public class MercuryFramebufferMixin {

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;getFirst()Ljava/lang/Object;"
        )
    )
    Object overrideFirstImageViewGet(
        List<HgImage.View> colorAttachments,
        Operation<Object> operation,
        @Local HgFramebuffer.CreateInfo createInfo
    ) {
        // if there are no color attachments, use depth attachment for FB size
        if (colorAttachments.size() == 0) {
            return createInfo.depthAttachment();
        }
        return operation.call(colorAttachments);
    }

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgImage;width()I"
        )
    )
    int overrideWidth(int width, @Local HgFramebuffer.CreateInfo createInfo) {
        HgImage.View firstColorAttachment;
        if (createInfo.colorAttachments().size() == 0) {
            firstColorAttachment = createInfo.depthAttachment();
        }
        else {
            firstColorAttachment = createInfo.colorAttachments().getFirst();
        }
        return width >> firstColorAttachment.baseMipLevel();
    }

    @ModifyExpressionValue(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgImage;height()I"
        )
    )
    int overrideHeight(int height, @Local HgFramebuffer.CreateInfo createInfo) {
        HgImage.View firstColorAttachment;
        if (createInfo.colorAttachments().size() == 0) {
            firstColorAttachment = createInfo.depthAttachment();
        }
        else {
            firstColorAttachment = createInfo.colorAttachments().getFirst();
        }
        return height >> firstColorAttachment.baseMipLevel();
    }

}
