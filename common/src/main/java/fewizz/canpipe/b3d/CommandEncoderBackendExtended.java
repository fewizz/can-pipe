package fewizz.canpipe.b3d;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public interface CommandEncoderBackendExtended extends CommandEncoderBackend {

    RenderPassBackend canpipe_createRenderPass(
        Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment
    );

    void canpipe_clearDepthTexture(
        GpuTexture texture, double depth,
        int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount  // added
    );

    void canpipe_clearColorTexture(
        GpuTexture texture, Vector4f color,
        int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount  // added
    );

    void canpipe_blitImage(
        GpuTexture srcTexture,
        GpuTexture dstTexture
    );

}
