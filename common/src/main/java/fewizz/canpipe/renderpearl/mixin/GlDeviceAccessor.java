package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.backend.opengl.GlDevice;

@Mixin(value = GlDevice.class, priority = 1001)
public interface GlDeviceAccessor {

    @Accessor("canpipe_pendingTextureViewBaseLayer") int get_canpipe_pendingTextureViewBaseLayer();
    @Accessor("canpipe_pendingTextureViewLayerCount") int get_canpipe_pendingTextureViewLayerCount();
    @Accessor("canpipe_addressModeW") AddressMode get_canpipe_addressModeW();
    @Accessor("canpipe_linearMipmap") Boolean get_canpipe_linearMipmap();
    @Accessor("canpipe_compareOp") CompareOp get_canpipe_compareOp();

}
