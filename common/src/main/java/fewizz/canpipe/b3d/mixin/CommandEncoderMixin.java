package fewizz.canpipe.b3d.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.b3d.CommandEncoderExtended;

@Mixin(CommandEncoder.class)
public class CommandEncoderMixin implements CommandEncoderExtended {

    @Final @Shadow private GpuDeviceBackend device;
    @Final @Shadow private CommandEncoderBackend backend;

    @Override
    public void canpipe_clearDepthTexture(GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearDepthTexture(texture, depth, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_clearColorTexture(GpuTexture texture, Vector4f color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearColorTexture(texture, color, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_blitImage(srcTexture, dstTexture);
    }

}
