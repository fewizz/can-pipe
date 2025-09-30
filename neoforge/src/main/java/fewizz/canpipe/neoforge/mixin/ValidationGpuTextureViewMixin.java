package fewizz.canpipe.neoforge.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuTextureViewExtended;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTextureView;

@Mixin(ValidationGpuTextureView.class)
public class ValidationGpuTextureViewMixin implements GpuTextureViewExtended {
    @Shadow @Final private GpuTextureView realTextureView;

    @Override
    public int canpipe_baseArrayLayer() {
        return ((GpuTextureViewExtended) this.realTextureView).canpipe_baseArrayLayer();
    }

    @Override
    public int canpipe_layerCount() {
        return ((GpuTextureViewExtended) this.realTextureView).canpipe_layerCount();
    }

}
