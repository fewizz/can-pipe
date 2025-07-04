package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.opengl.GlTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;

@Mixin(GlTextureView.class)
public class GlTextureViewMixin implements GpuTextureViewExtended {

    @Unique int canpipe_baseArrayLayer;
    @Unique int canpipe_layerCount;

    @Override
    public int canpipe_baseArrayLayer() {
        return this.canpipe_baseArrayLayer;
    }

    @Override
    public int canpipe_layerCount() {
        return this.canpipe_layerCount;
    }

}
