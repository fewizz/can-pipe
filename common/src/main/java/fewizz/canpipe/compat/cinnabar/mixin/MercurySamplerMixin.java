package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.textures.FilterMode;

import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.core.mercury.MercuryDevice;
import graphics.cinnabar.core.mercury.MercurySampler;

@Mixin(MercurySampler.class)
public class MercurySamplerMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;maxLod(F)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    float disableMaxLodClamp(float maxLod, @Local MercuryDevice device) {
        return VK10.VK_LOD_CLAMP_NONE;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;compareEnable(Z)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    boolean fillCompareEnable(boolean compareEnable, @Local HgSampler.CreateInfo createInfo) {
        return compareEnable || createInfo.compareOp() != null;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;mipmapMode(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    int fillMipmapMode(int mipmapMode, @Local MercuryDevice device) {
        FilterMode canpipe_mipmapMode = ((MercuryDeviceAccessor) device).get_canpipe_samplerMipFilter();
        if (canpipe_mipmapMode != null) {
            mipmapMode = switch (canpipe_mipmapMode) {
                case NEAREST -> VK10.VK_FILTER_NEAREST;
                case LINEAR -> VK10.VK_FILTER_LINEAR;
            };
        }
        return mipmapMode;
    }

}
