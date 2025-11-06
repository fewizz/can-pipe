package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.CommandEncoderExtended;
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
public abstract class Hg3DCommandEncoderMixin implements CommandEncoderExtended {

    @Shadow @Final private Hg3DGpuDevice device;

    @Shadow HgCommandBuffer mainCommandBuffer() { return null; }
    @Shadow public Hg3DRenderPass createRenderPass(Supplier<String> debugGroup, HgRenderPass renderpass, HgFramebuffer framebuffer) { return null; }

    @Override
    public RenderPass canpipe_createRenderPass(
        Supplier<String> supplier,
        GpuTextureView[] colorAttachments,
        @Nullable GpuTextureView depthAttachment
    ) {
        try {
            HgRenderPass hgRenderPass = ((Hg3DGpuDeviceAccessor) this.device).canpipe_getRenderPass(
                Arrays.stream(colorAttachments).map(a -> Hg3DConst.format(a.texture().getFormat())).toList(),
                depthAttachment != null ? Hg3DConst.format(depthAttachment.texture().getFormat()) : null
            );

            var framebuffers = ((Hg3DGpuDeviceAccessor) this.device).get_canpipe_framebuffers();
            List<HgImage.View> imageViews = Arrays.stream(colorAttachments).map(a -> ((Hg3DGpuTextureView) a).imageView()).toList();
            HgImage.View depthView = depthAttachment != null ? ((Hg3DGpuTextureView) depthAttachment).imageView() : null;

            var framebuffer = framebuffers.computeIfAbsent(Pair.of(imageViews, depthView), k -> {
                HgFramebuffer.CreateInfo createInfo = new HgFramebuffer.CreateInfo(hgRenderPass, imageViews, depthView);
                return this.device.hgDevice().createFramebuffer(createInfo);
            });

            Hg3DRenderPass renderPass = this.createRenderPass(supplier, hgRenderPass, framebuffer);

            return renderPass;
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
