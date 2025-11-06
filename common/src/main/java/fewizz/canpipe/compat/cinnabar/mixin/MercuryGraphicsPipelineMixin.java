package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.core.mercury.MercuryGraphicsPipeline;

@Mixin(MercuryGraphicsPipeline.class)
public class MercuryGraphicsPipelineMixin {

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkPipelineColorBlendAttachmentState$Buffer;colorWriteMask(I)Lorg/lwjgl/vulkan/VkPipelineColorBlendAttachmentState$Buffer;",
            shift = Shift.AFTER
        )
    )
    void fixBlendAttachmentBufferIncrement(
        CallbackInfo ci, @Local VkPipelineColorBlendAttachmentState.Buffer blendAttachment
    ) {
        blendAttachment.position(blendAttachment.position() + 1);
    }

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkPipelineColorBlendStateCreateInfo;pAttachments(Lorg/lwjgl/vulkan/VkPipelineColorBlendAttachmentState$Buffer;)Lorg/lwjgl/vulkan/VkPipelineColorBlendStateCreateInfo;"
        )
    )
    void rewindBlendAttachmentBuffer(
        CallbackInfo ci, @Local VkPipelineColorBlendAttachmentState.Buffer blendAttachment
    ) {
        blendAttachment.position(0);
    }

}
