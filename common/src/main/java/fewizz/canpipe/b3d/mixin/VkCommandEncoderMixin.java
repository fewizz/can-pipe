package fewizz.canpipe.b3d.mixin;

import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkOffset3D;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;
import com.mojang.blaze3d.vulkan.VulkanConst;
import com.mojang.blaze3d.vulkan.VulkanGpuTexture;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;

@Mixin(VulkanCommandEncoder.class)
public abstract class VkCommandEncoderMixin implements CommandEncoderBackendExtended {

    @Unique private int canpipe_clearBaseLevel = -1;
    @Unique private int canpipe_clearLevelCount = -1;
    @Unique private int canpipe_clearBaseLayer = -1;
    @Unique private int canpipe_clearLayerCount = -1;

    @Shadow private VkCommandBuffer commandBuffer() { return null; }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearDepthTexture(texture, depth);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    @Override
    public void canpipe_clearColorTexture(
        GpuTexture texture, Vector4f color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    ) {
        try {
            this.canpipe_clearBaseLevel = baseMipLevel;
            this.canpipe_clearLevelCount = levelCount;
            this.canpipe_clearBaseLayer = baseArrayLayer;
            this.canpipe_clearLayerCount = layerCount;
            this.clearColorTexture(texture, color);
        }
        finally {
            this.canpipe_clearBaseLevel = -1;
            this.canpipe_clearLevelCount = -1;
            this.canpipe_clearBaseLayer = -1;
            this.canpipe_clearLayerCount = -1;
        }
    }

    public void canpipe_blitImage(
        GpuTexture srcTexture,
        GpuTexture dstTexture
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var imageSubresourceRange = VkImageSubresourceRange.calloc(stack);
            imageSubresourceRange.aspectMask(VK12.VK_IMAGE_ASPECT_COLOR_BIT);
            imageSubresourceRange.baseMipLevel(0);
            imageSubresourceRange.levelCount(1);
            imageSubresourceRange.baseArrayLayer(0);
            imageSubresourceRange.layerCount(1);

            var imageBarrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default();
            imageBarrier.srcAccessMask(VK12.VK_ACCESS_MEMORY_READ_BIT | VK12.VK_ACCESS_MEMORY_WRITE_BIT);
            imageBarrier.dstAccessMask(VK12.VK_ACCESS_MEMORY_READ_BIT | VK12.VK_ACCESS_MEMORY_WRITE_BIT);
            imageBarrier.dstQueueFamilyIndex(VK12.VK_QUEUE_FAMILY_IGNORED);
            imageBarrier.srcQueueFamilyIndex(VK12.VK_QUEUE_FAMILY_IGNORED);
            imageBarrier.subresourceRange(imageSubresourceRange);
            imageBarrier.dstAccessMask(VK12.VK_ACCESS_TRANSFER_READ_BIT);
            imageBarrier.oldLayout(VK12.VK_IMAGE_LAYOUT_GENERAL);
            imageBarrier.newLayout(VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            imageBarrier.image(((VulkanGpuTexture) dstTexture).vkImage());
            // wait for nothing, this is just the swapchain image
            VK12.vkCmdPipelineBarrier(this.commandBuffer(), VK12.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK12.VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, imageBarrier);

            final var regions = VkImageBlit.calloc(1, stack);

            final var srcSubresourceLayer = VkImageSubresourceLayers.calloc(stack);
            srcSubresourceLayer.layerCount(1);
            srcSubresourceLayer.aspectMask(VulkanConst.formatAspectMask(srcTexture.getFormat()));

            final var dstSubresourceLayer = VkImageSubresourceLayers.calloc(stack);
            dstSubresourceLayer.layerCount(1);
            dstSubresourceLayer.aspectMask(VulkanConst.formatAspectMask(dstTexture.getFormat()));

            regions.srcSubresource(srcSubresourceLayer);
            regions.srcOffsets(0, VkOffset3D.calloc(stack).x(0).y(0).z(0));
            regions.srcOffsets(1, VkOffset3D.calloc(stack).x(srcTexture.getWidth(0)).y(srcTexture.getHeight(0)).z(1));

            regions.dstSubresource(srcSubresourceLayer);
            regions.dstOffsets(0, VkOffset3D.calloc(stack).x(0).y(0).z(0));
            regions.dstOffsets(1, VkOffset3D.calloc(stack).x(dstTexture.getWidth(0)).y(dstTexture.getHeight(0)).z(1));

            VK12.vkCmdBlitImage(
                this.commandBuffer(),
                ((VulkanGpuTexture) srcTexture).vkImage(), VK12.VK_IMAGE_LAYOUT_GENERAL,
                ((VulkanGpuTexture) dstTexture).vkImage(), VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                regions,
                VK12.VK_FILTER_NEAREST
            );

            imageBarrier.oldLayout(VK12.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            imageBarrier.newLayout(VK12.VK_IMAGE_LAYOUT_GENERAL);
            imageBarrier.srcAccessMask(VK12.VK_ACCESS_TRANSFER_WRITE_BIT);
            imageBarrier.dstAccessMask(0);

            VK12.vkCmdPipelineBarrier(this.commandBuffer(), VK12.VK_PIPELINE_STAGE_TRANSFER_BIT, VK12.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, null, null, imageBarrier);
        }
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseMipLevel(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearBaseLevel(int baseLevel) {
        return this.canpipe_clearBaseLevel != -1 ? this.canpipe_clearBaseLevel : baseLevel;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;levelCount(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearLevelCount(int levelCount) {
        return this.canpipe_clearLevelCount != -1 ? this.canpipe_clearLevelCount : levelCount;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;baseArrayLayer(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearBaseLayer(int baseLayer) {
        return this.canpipe_clearBaseLayer != -1 ? this.canpipe_clearBaseLayer : baseLayer;
    }

    @ModifyArg(
        method = {"clearDepthTextureUnsynced", "clearColorTextureUnsynced"},
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/vulkan/VkImageSubresourceRange;layerCount(I)Lorg/lwjgl/vulkan/VkImageSubresourceRange;"
        )
    )
    int setClearLayerCount(int layerCount) {
        return this.canpipe_clearLayerCount != -1 ? this.canpipe_clearLayerCount : layerCount;
    }

}
