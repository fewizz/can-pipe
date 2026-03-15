package fewizz.canpipe.b3d.mixin;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.b3d.CommandEncoderExtended;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandEncoder.class)
public class CommandEncoderMixin implements CommandEncoderExtended {

    @Final @Shadow private GpuDeviceBackend device;
    @Final @Shadow private CommandEncoderBackend backend;

    @Override
    public RenderPass canpipe_createRenderPass(Supplier<String> supplier, GpuTextureView[] colorAttachments, @Nullable GpuTextureView depthAttachment) {
        RenderPassBackend renderPassBackend = ((CommandEncoderBackendExtended) this.backend).canpipe_createRenderPass(supplier, colorAttachments, depthAttachment);
        return new RenderPass(renderPassBackend, this.device);
    }

    @Override
    public void canpipe_clearDepthTexture(GpuTexture texture, double depth, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearDepthTexture(texture, depth, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_clearColorTexture(GpuTexture texture, int color, int baseMipLevel, int levelCount, int baseArrayLayer, int layerCount) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_clearColorTexture(texture, color, baseMipLevel, levelCount, baseArrayLayer, layerCount);
    }

    @Override
    public void canpipe_blitImage(GpuTexture srcTexture, GpuTexture dstTexture) {
        ((CommandEncoderBackendExtended) this.backend).canpipe_blitImage(srcTexture, dstTexture);
    }

}
