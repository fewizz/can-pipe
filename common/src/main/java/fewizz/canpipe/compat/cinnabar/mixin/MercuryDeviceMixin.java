package fewizz.canpipe.compat.cinnabar.mixin;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.shaders.ShaderType;

import graphics.cinnabar.core.mercury.MercuryDevice;

@Mixin(MercuryDevice.class)
public class MercuryDeviceMixin {

    @Unique private TriConsumer<String, ShaderType, String> canpipe_onCompilationError = null;

}
