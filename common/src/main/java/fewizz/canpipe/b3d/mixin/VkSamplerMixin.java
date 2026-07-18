package fewizz.canpipe.b3d.mixin;

import org.jspecify.annotations.Nullable;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanGpuSampler;

@Mixin(VulkanGpuSampler.class)
public class VkSamplerMixin {

    @Unique protected AddressMode canpipe_addressModeW;
    @Unique protected boolean canpipe_linearMipmap;
    @Unique @Nullable protected CompareOp canpipe_compareOp = null;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VK12;vkCreateSampler("+
                "Lorg/lwjgl/vulkan/VkDevice;"+
                "Lorg/lwjgl/vulkan/VkSamplerCreateInfo;"+
                "Lorg/lwjgl/vulkan/VkAllocationCallbacks;"+
                "Ljava/nio/LongBuffer;"+
            ")I"
        )
    )
    void extend(CallbackInfo ci, @Local VkSamplerCreateInfo createInfo) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();

        var addressModeW = ((VkDeviceAccessor) device).get_canpipe_addressModeW();
        this.canpipe_addressModeW = addressModeW != null ? addressModeW : AddressMode.REPEAT;

        var linearMipmap = ((VkDeviceAccessor) device).get_canpipe_linearMipmap();
        this.canpipe_linearMipmap = linearMipmap != null ? linearMipmap : true;

        this.canpipe_compareOp = ((VkDeviceAccessor) device).get_canpipe_compareOp();

        createInfo.addressModeW(VulkanConst.toVk(this.canpipe_addressModeW));

        if (this.canpipe_compareOp != null) {
            createInfo.compareEnable(true);
            createInfo.compareOp(VulkanConst.toVk(this.canpipe_compareOp));
        }

    }

}
