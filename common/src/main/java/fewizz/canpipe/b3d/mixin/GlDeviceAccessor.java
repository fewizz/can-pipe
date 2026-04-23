package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

@Mixin(value = GlDevice.class, priority = 1001)
public interface GlDeviceAccessor {

    // key - color attachemnt + (nullable) depth attachment, value = framebuffer ID
    @Accessor("canpipe_framebufferCache") Object2IntMap<Pair<List<GpuTextureView>, GpuTextureView>> get_canpipe_framebufferCache();

    @Accessor("canpipe_pendingTextureViewBaseLayer") int get_canpipe_pendingTextureViewBaseLayer();

    @Accessor("canpipe_pendingTextureViewLayerCount") int get_canpipe_pendingTextureViewLayerCount();

    @Accessor("canpipe_addressModeW") AddressMode get_canpipe_addressModeW();

    @Accessor("canpipe_compareOp") CompareOp get_canpipe_compareOp();

    @Accessor("canpipe_linearMipmap") Boolean get_canpipe_linearMipmap();

}
