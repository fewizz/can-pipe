package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkOffset3D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fewizz.canpipe.compat.cinnabar.HgCommandBufferExtended;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.memory.GrowingMemoryStack;
import graphics.cinnabar.core.mercury.MercuryCommandBuffer;
import graphics.cinnabar.core.mercury.MercuryImage;

@Mixin(MercuryCommandBuffer.class)
public abstract class MercuryCommandBufferMixin implements HgCommandBufferExtended {

    @Shadow @Final private VkCommandBuffer commandBuffer;
    @Shadow @Final private GrowingMemoryStack memoryStack;

    @Override
    public void canpipe_blitImage(
        HgImage srcImage,
        HgImage dstImage
    ) {
        try (final var stack = memoryStack.push()) {
            final var imageBarrier = VkImageMemoryBarrier.calloc(1, stack).sType$Default();
            imageBarrier.srcAccessMask(VK10.VK_ACCESS_MEMORY_READ_BIT | VK10.VK_ACCESS_MEMORY_WRITE_BIT);
            imageBarrier.dstAccessMask(VK10.VK_ACCESS_MEMORY_READ_BIT | VK10.VK_ACCESS_MEMORY_WRITE_BIT);
            imageBarrier.dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            imageBarrier.srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            final var imageSubresourceRange = VkImageSubresourceRange.calloc(stack);
            imageSubresourceRange.aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            imageSubresourceRange.baseMipLevel(0);
            imageSubresourceRange.levelCount(1);
            imageSubresourceRange.baseArrayLayer(0);
            imageSubresourceRange.layerCount(1);
            imageBarrier.subresourceRange(imageSubresourceRange);
            
            imageBarrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);
            imageBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_GENERAL);
            imageBarrier.newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            imageBarrier.image(((MercuryImage) dstImage).vkImage());
            // wait for nothing, this is just the swapchain image
            VK10.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, imageBarrier);

            final var regions = VkImageBlit.calloc(1, stack);

            final var srcSubresourceLayer = VkImageSubresourceLayers.calloc(stack);
            srcSubresourceLayer.layerCount(1);
            srcSubresourceLayer.aspectMask(srcImage.format().aspects());

            final var dstSubresourceLayer = VkImageSubresourceLayers.calloc(stack);
            dstSubresourceLayer.layerCount(1);
            dstSubresourceLayer.aspectMask(dstImage.format().aspects());

            regions.srcSubresource(srcSubresourceLayer);
            regions.srcOffsets(0, VkOffset3D.calloc(stack).x(0).y(0).z(0));
            regions.srcOffsets(1, VkOffset3D.calloc(stack).x(srcImage.width()).y(srcImage.height()).z(1));

            regions.dstSubresource(srcSubresourceLayer);
            regions.dstOffsets(0, VkOffset3D.calloc(stack).x(0).y(0).z(0));
            regions.dstOffsets(1, VkOffset3D.calloc(stack).x(dstImage.width()).y(dstImage.height()).z(1));

            VK10.vkCmdBlitImage(
                this.commandBuffer,
                ((MercuryImage) srcImage).vkImage(), VK10.VK_IMAGE_LAYOUT_GENERAL,
                ((MercuryImage) dstImage).vkImage(), VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                regions,
                VK10.VK_FILTER_NEAREST
            );

            imageBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            imageBarrier.newLayout(VK10.VK_IMAGE_LAYOUT_GENERAL);
            imageBarrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
            imageBarrier.dstAccessMask(0);

            VK10.vkCmdPipelineBarrier(commandBuffer, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, 0, null, null, imageBarrier);
        }
    }

}
