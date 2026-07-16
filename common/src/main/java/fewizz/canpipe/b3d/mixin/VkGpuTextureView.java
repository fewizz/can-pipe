package fewizz.canpipe.b3d.mixin;

import org.lwjgl.vulkan.VK12;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanGpuTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(VulkanGpuTextureView.class)
public class VkGpuTextureView implements GpuTextureViewExtended {

    @Unique /*final*/ int canpipe_baseArrayLayer;
    @Unique /*final*/ int canpipe_layerCount;

    @Override public int canpipe_baseArrayLayer() { return this.canpipe_baseArrayLayer; }
    @Override public int canpipe_layerCount() { return this.canpipe_layerCount; }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageViewCreateInfo;viewType(I)Lorg/lwjgl/vulkan/VkImageViewCreateInfo;"
        ),
        index = 0
    )
    int onSetViewType(int value) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        int layerCount = ((VkDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
        if (value == VK12.VK_IMAGE_VIEW_TYPE_CUBE && layerCount != -1 && layerCount < 6) {
            value = VK12.VK_IMAGE_VIEW_TYPE_2D;
        }
        if (value == VK12.VK_IMAGE_VIEW_TYPE_2D && layerCount > 1) {
            value = VK12.VK_IMAGE_VIEW_TYPE_2D_ARRAY;
        }
        return value;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseArrayLayer(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        ),
        index = 0
    )
    int onSetBaseArrayLayer(int value) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        int baseArrayLayer = ((VkDeviceAccessor) device).get_canpipe_pendingTextureViewBaseLayer();
        this.canpipe_baseArrayLayer = baseArrayLayer != -1 ? baseArrayLayer : value;
        return this.canpipe_baseArrayLayer;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;layerCount(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        ),
        index = 0
    )
    int onSetLayerCount(int value) {
        GpuDeviceBackend device = ((GpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getBackend();
        int layerCount = ((VkDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
        this.canpipe_layerCount = layerCount != -1 ? layerCount : value;
        return this.canpipe_layerCount;
    }

}
