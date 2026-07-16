package fewizz.canpipe.b3d.mixin;

import org.lwjgl.vulkan.VK12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;

@Mixin(VulkanGpuTexture.class)
public class VkGpuTextureMixin {

    /*@ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageCreateInfo;imageType(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        ),
        index = 0
    )
    int onSetBaseArrayLayer(int value, @Local(name = "depthOrLayers") int depthOrLayers) {
        if (depthOrLayers > 1) {
            value = VK12.VK_IMAGE_TYPE_3D;
        }
        return value;
    }*/

}
