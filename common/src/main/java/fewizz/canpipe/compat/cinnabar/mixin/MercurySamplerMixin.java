package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;

import graphics.cinnabar.api.hg.HgSampler;
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

}
