package fewizz.canpipe.compat.cinnabar.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.b3d.GpuTextureViewExtended;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuTextureView;

@Mixin(Hg3DGpuTextureView.class)
public class Hg3DGpuTextureViewMixin implements GpuTextureViewExtended {
    @Final private HgImage.View imageView;

    @Override
    public int canpipe_baseArrayLayer() {
        return this.imageView.baseArrayLayer();
    }

    @Override
    public int canpipe_layerCount() {
        return this.imageView.layerCount();
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgImage;createView("+
                "Lgraphics/cinnabar/api/hg/HgImage$View$Type;"+
                "Lgraphics/cinnabar/api/hg/enums/HgFormat;"+
                "IIII"+
            ")Lgraphics/cinnabar/api/hg/HgImage$View;"
        ),
        index = 4
    )
    int overrideBaseLayer(int baseLayer) {
        var device = (Hg3DGpuDevice) RenderSystem.getDevice();
        int baseLayerOverride = ((Hg3DGpuDeviceAccessor) device).get_canpipe_pendingTextureViewBaseLayer();
        if (baseLayerOverride != -1) {
            baseLayer = baseLayerOverride;
        }
        return baseLayer;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgImage;createView("+
                "Lgraphics/cinnabar/api/hg/HgImage$View$Type;"+
                "Lgraphics/cinnabar/api/hg/enums/HgFormat;"+
                "IIII"+
            ")Lgraphics/cinnabar/api/hg/HgImage$View;"
        ),
        index = 5
    )
    int overrideArrayLayerCount(int layerCount) {
        var device = (Hg3DGpuDevice) RenderSystem.getDevice();
        int layerCountOverride = ((Hg3DGpuDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
        if (layerCountOverride != -1) {
            layerCount = layerCountOverride;
        }
        return layerCount;
    }

    @ModifyArg(
        method = "getFramebuffer",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgFramebuffer$CreateInfo;<init>("+
                "Lgraphics/cinnabar/api/hg/HgRenderPass;"+
                "Ljava/util/List;"+
                "Lgraphics/cinnabar/api/hg/HgImage$View;"+
            ")V"
        )
    )
    List<HgImage.View> overrideColorAttachments(List<HgImage.View> colorAttachments) {
        var device = (Hg3DGpuDevice) RenderSystem.getDevice();
        var colorAttachmentsOverride = ((Hg3DGpuDeviceAccessor) device).get_canpipe_pendingColorAttachments();

        if (colorAttachmentsOverride != null) {
            colorAttachments = new ArrayList<HgImage.View>();

            for (var a : colorAttachmentsOverride) {
                colorAttachments.add(((Hg3DGpuTextureView) a).imageView());
            }
        }

        return colorAttachments;
    }

}
