package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import graphics.cinnabar.api.hg.HgCommandBuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.core.hg3d.Hg3DCommandEncoder;
import graphics.cinnabar.core.hg3d.Hg3DGpuTexture;

@Mixin(Hg3DCommandEncoder.class)
public abstract class Hg3DCommandEncoderMixin implements CommandEncoderExtended {

    abstract @Shadow HgCommandBuffer mainCommandBuffer();

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier,
        GpuTextureView[] colorAttachments,
        @Nullable GpuTextureView depthAttachment
    ) {
        try {
            ((Hg3DGpuDeviceAccessor) RenderSystem.getDevice()).set_canpipe_pendingColorAttachments(colorAttachments);
            return this.createRenderPass(
                supplier,
                colorAttachments.length > 0 ? colorAttachments[0] : depthAttachment,
                OptionalInt.empty(),
                depthAttachment,
                OptionalDouble.empty()
            );
        } finally {
            ((Hg3DGpuDeviceAccessor) RenderSystem.getDevice()).set_canpipe_pendingColorAttachments(null);
        }
    }

    @Override
    public void canpipe_clearDepthTexture(
        GpuTexture texture,
        double depth,
        int baseMipLevel,
        int levelCount,
        int baseArrayLayer,
        int layerCount
    ) {
        HgCommandBuffer cb = this.mainCommandBuffer();
        cb.barrier();
        cb.clearDepthStencilImage(
            new HgImage.ResourceRange(
                ((Hg3DGpuTexture) texture).image(),
                baseMipLevel,
                levelCount,
                baseArrayLayer,
                layerCount
            ),
            depth,
            -1
        );
    }

    @Override
    public void canpipe_clearColorTexture(
        GpuTexture texture,
        int color,
        int baseMipLevel,
        int levelCount,
        int baseArrayLayer,
        int layerCount
    ) {
        HgCommandBuffer cb = this.mainCommandBuffer();
        cb.barrier();
        cb.clearColorImage(
            new HgImage.ResourceRange(
                ((Hg3DGpuTexture) texture).image(),
                baseMipLevel,
                levelCount,
                baseArrayLayer,
                layerCount
            ),
            color
        );
    }
    
}
