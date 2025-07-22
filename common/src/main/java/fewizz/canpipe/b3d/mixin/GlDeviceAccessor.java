package fewizz.canpipe.b3d.mixin;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

@Mixin(value = GlDevice.class, priority = 1001)
public interface GlDeviceAccessor {

    @Accessor("canpipe_framebufferCache")
    Object2IntMap<Pair<GpuTextureView[], GlTextureView>> get_canpipe_framebufferCache();

    @Accessor("canpipe_pendingTextureViewBaseLayer")
    int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount")
    int get_canpipe_pendingTextureViewLayerCount();

}
