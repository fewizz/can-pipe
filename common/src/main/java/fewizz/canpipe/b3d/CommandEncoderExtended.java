package fewizz.canpipe.b3d;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface CommandEncoderExtended extends CommandEncoder {

    RenderPass canpipe_createRenderPass(
        Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment
    );

    // Simpler version of https://registry.khronos.org/vulkan/specs/latest/man/html/vkCmdClearDepthStencilImage.html
    void canpipe_clearDepthTexture(
        GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    );

    void canpipe_clearColorTexture(
        GpuTexture texture, int color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount
    );

}
