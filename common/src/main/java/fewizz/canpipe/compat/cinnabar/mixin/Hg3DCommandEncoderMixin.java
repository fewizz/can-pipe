package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.compat.cinnabar.HgCommandBufferExtended;
import graphics.cinnabar.api.hg.HgCommandBuffer;
import graphics.cinnabar.api.hg.HgFramebuffer;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.api.hg.HgRenderPass;
import graphics.cinnabar.core.hg3d.Hg3DCommandEncoder;
import graphics.cinnabar.core.hg3d.Hg3DCommandEncoder.Hg3DRenderPass;
import graphics.cinnabar.core.hg3d.Hg3DConst;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuTexture;
import graphics.cinnabar.core.hg3d.Hg3DGpuTextureView;

@Mixin(Hg3DCommandEncoder.class)
public abstract class Hg3DCommandEncoderMixin implements CommandEncoderBackendExtended {

    @Shadow @Final private Hg3DGpuDevice device;

    @Shadow HgCommandBuffer mainCommandBuffer() { return null; }
    @Shadow public Hg3DRenderPass createRenderPass(Supplier<String> debugGroup, HgRenderPass renderpass, HgFramebuffer framebuffer) { return null; }

    @Override
    public RenderPassBackend canpipe_createRenderPass(
        Supplier<String> supplier,
        GpuTextureView[] colorAttachments,
        @Nullable GpuTextureView depthAttachment
    ) {
        var canpipe_renderPasses = ((Hg3DGpuDeviceAccessor) this.device).get_canpipe_renderPasses();
        var canpipe_framebuffers = ((Hg3DGpuDeviceAccessor) this.device).get_canpipe_framebuffers();

        var colorFormats = Arrays.stream(colorAttachments).map(a -> Hg3DConst.format(a.texture().getFormat())).toList();
        var imageViews = Arrays.stream(colorAttachments).map(a -> ((Hg3DGpuTextureView) a).imageView()).toList();

        var depthFormat = depthAttachment != null ? Hg3DConst.format(depthAttachment.texture().getFormat()) : null;
        var depthView = depthAttachment != null ? ((Hg3DGpuTextureView) depthAttachment).imageView() : null;

        HgRenderPass hgRenderPass = canpipe_renderPasses.computeIfAbsent(
            Pair.of(colorFormats, depthFormat),
            k -> {
                var createInfo = new HgRenderPass.CreateInfo(colorFormats, depthFormat);
                return this.device.hgDevice().createRenderPass(createInfo);
            }
        );

        var framebuffer = canpipe_framebuffers.computeIfAbsent(
            Pair.of(imageViews, depthView),
            k -> {
                var createInfo = new HgFramebuffer.CreateInfo(hgRenderPass, imageViews, depthView);
                return this.device.hgDevice().createFramebuffer(createInfo);
            }
        );

        Hg3DRenderPass renderPass = this.createRenderPass(supplier, hgRenderPass, framebuffer);
        return renderPass;
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

    @Override
    public void canpipe_blitImage(
        GpuTexture srcTexture,
        GpuTexture dstTexture
    ) {
        HgCommandBuffer cb = this.mainCommandBuffer();
        ((HgCommandBufferExtended) cb).canpipe_blitImage(
            ((Hg3DGpuTexture) srcTexture).image(),
            ((Hg3DGpuTexture) dstTexture).image()
        );
    }

}
