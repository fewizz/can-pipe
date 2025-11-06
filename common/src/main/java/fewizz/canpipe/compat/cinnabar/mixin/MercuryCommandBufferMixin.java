package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.core.mercury.MercuryCommandBuffer;

@Mixin(MercuryCommandBuffer.class)
public class MercuryCommandBufferMixin {

    @ModifyArg(
        method = {"clearColorImage", "clearDepthStencilImage"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseMipLevel(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        ),
        index = 0
    )
    int fixClearImageBaseLevel(int level, @Local HgImage.ResourceRange range) {
        assert level == 0;
        return range.baseMipLevel();
    }

    @ModifyArg(
        method = {"clearColorImage", "clearDepthStencilImage"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseArrayLayer(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        ),
        index = 0
    )
    int fixClearImageBaseLayer(int layer, @Local HgImage.ResourceRange range) {
        assert layer == 0;
        return range.baseArrayLayer();
    }

}
