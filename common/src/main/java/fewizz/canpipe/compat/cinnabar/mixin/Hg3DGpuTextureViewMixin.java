package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.GpuDeviceBackend;

import fewizz.canpipe.b3d.GpuTextureViewExtended;
import fewizz.canpipe.b3d.RealGpuDeviceProviderService;
import graphics.cinnabar.api.hg.HgImage;
import graphics.cinnabar.core.hg3d.Hg3DGpuTexture;
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
        index = 0
    )
    HgImage.View.Type overrideViewType(HgImage.View.Type viewType, @Local Hg3DGpuTexture texture) {
        boolean cubemap = (texture.usage() & Hg3DGpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0;

        GpuDeviceBackend device = RealGpuDeviceProviderService.getRealGpuDeviceBackend();
        int layerCountOverride = ((Hg3DGpuDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();

        viewType = HgImage.View.Type.TYPE_2D;

        if (cubemap && (layerCountOverride == -1 || layerCountOverride % 6 == 0)) {
            viewType = HgImage.View.Type.TYPE_CUBE;
        }
        else if ((layerCountOverride == -1 && texture.getDepthOrLayers() > 1) || layerCountOverride > 1) {
            viewType = HgImage.View.Type.TYPE_2D_ARRAY;
        }

        return viewType;
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
        GpuDeviceBackend device = RealGpuDeviceProviderService.getRealGpuDeviceBackend();
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
        GpuDeviceBackend device = RealGpuDeviceProviderService.getRealGpuDeviceBackend();
        int layerCountOverride = ((Hg3DGpuDeviceAccessor) device).get_canpipe_pendingTextureViewLayerCount();
        if (layerCountOverride != -1) {
            layerCount = layerCountOverride;
        }
        return layerCount;
    }

}
