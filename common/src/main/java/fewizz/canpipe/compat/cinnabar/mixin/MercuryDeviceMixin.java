package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.textures.FilterMode;

import graphics.cinnabar.core.mercury.MercuryDevice;

@Mixin(MercuryDevice.class)
public class MercuryDeviceMixin {

    @Unique private FilterMode canpipe_samplerMipFilter;

}
