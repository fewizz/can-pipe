package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.FilterMode;

import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.core.mercury.MercuryDevice;
import graphics.cinnabar.core.mercury.MercurySampler;

@Mixin(MercurySampler.class)
public class MercurySamplerMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;addressModeU(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    int fixAddressModeU(int u, @Local HgSampler.CreateInfo createInfo) {
        assert u == 0;
        u = createInfo.addressU();
        return u;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;addressModeV(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    int fixAddressModeV(int v, @Local HgSampler.CreateInfo createInfo) {
        assert v == 0;
        v = createInfo.addressV();
        return v;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;addressModeW(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    int fixAddressModeW(int w, @Local HgSampler.CreateInfo createInfo) {
        assert w == 0;
        w = createInfo.addressW();
        return w;
    }

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
    boolean fillCompareEnable(boolean compareEnable, @Local MercuryDevice device) {
        DepthTestFunction canpipe_compareOp = ((MercuryDeviceAccessor) device).get_canpipe_samplerCompareOp();
        if (canpipe_compareOp != null) {
            compareEnable = true;
        }
        return compareEnable;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;compareOp(I)Lorg/lwjgl/vulkan/VkSamplerCreateInfo;")
    )
    int fillCompareOp(int compareOp, @Local MercuryDevice device) {
        DepthTestFunction canpipe_compareOp = ((MercuryDeviceAccessor) device).get_canpipe_samplerCompareOp();
        if (canpipe_compareOp != null) {
            compareOp = switch (canpipe_compareOp) {
                case NO_DEPTH_TEST -> VK10.VK_COMPARE_OP_ALWAYS;
                case EQUAL_DEPTH_TEST -> VK10.VK_COMPARE_OP_EQUAL;
                case LEQUAL_DEPTH_TEST -> VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
                case LESS_DEPTH_TEST -> VK10.VK_COMPARE_OP_LESS;
                case GREATER_DEPTH_TEST -> VK10.VK_COMPARE_OP_GREATER;
            };
        }
        return compareOp;
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
