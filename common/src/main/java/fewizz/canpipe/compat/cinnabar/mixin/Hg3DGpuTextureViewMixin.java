package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;

import fewizz.canpipe.b3d.GpuTextureViewExtended;
import graphics.cinnabar.api.hg.HgImage;
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

}
