package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.textures.GpuTextureView;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

@Mixin(value = GlDevice.class, priority = 1001)
public interface GlDeviceAccessor {

    @Accessor("canpipe_framebufferCache")
    Object2IntMap<List<GpuTextureView>> get_canpipe_framebufferCache();

}
