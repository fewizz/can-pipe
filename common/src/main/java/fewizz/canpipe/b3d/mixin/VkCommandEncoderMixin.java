package fewizz.canpipe.b3d.mixin;

import java.util.function.Supplier;

import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vulkan.VulkanCommandEncoder;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;

@Mixin(VulkanCommandEncoder.class)
public abstract class VkCommandEncoderMixin implements CommandEncoderBackendExtended {

    public RenderPassBackend canpipe_createRenderPass(
        Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment
    ) {
        
    }

    public void canpipe_clearDepthTexture(
        GpuTexture texture, double depth,
        int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount  // added
    ) {

    }

    public void canpipe_clearColorTexture(
        GpuTexture texture, Vector4f color,
        int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount  // added
    ) {

    }

    public void canpipe_blitImage(
        GpuTexture srcTexture,
        GpuTexture dstTexture
    ) {
        
    }

}
