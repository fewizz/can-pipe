package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VK10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;

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

}
