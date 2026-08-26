package fewizz.canpipe.renderpearl;

import org.joml.Vector4f;

import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;

public interface CommandEncoderBackendExtended extends CommandEncoderBackend {

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
