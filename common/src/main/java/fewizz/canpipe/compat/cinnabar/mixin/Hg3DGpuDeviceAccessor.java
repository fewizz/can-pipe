package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.textures.GpuTextureView;

import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;

@Mixin(Hg3DGpuDevice.class)
public interface Hg3DGpuDeviceAccessor {

    @Accessor("canpipe_pendingColorAttachments")
    void set_canpipe_pendingColorAttachments(GpuTextureView[] colors);

    @Accessor("canpipe_pendingColorAttachments")
    GpuTextureView[] get_canpipe_pendingColorAttachments();

    @Accessor("canpipe_pendingTextureViewBaseLayer")
    int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount")
    int get_canpipe_pendingTextureViewLayerCount();
    
}
