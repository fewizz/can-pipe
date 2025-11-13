package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.textures.FilterMode;

import graphics.cinnabar.core.mercury.MercuryDevice;

@Mixin(MercuryDevice.class)
public interface MercuryDeviceAccessor {

    @Accessor("canpipe_samplerMipFilter") FilterMode get_canpipe_samplerMipFilter();
    @Accessor("canpipe_samplerMipFilter") void set_canpipe_samplerMipFilter(FilterMode mipFilter);

}
