package fewizz.canpipe.compat.cinnabar.mixin;

import org.apache.commons.lang3.function.TriConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.shaders.ShaderType;

import graphics.cinnabar.core.mercury.MercuryDevice;


@Mixin(MercuryDevice.class)
public interface MercuryDeviceAccessor {

    @Accessor("canpipe_onCompilationError")
    TriConsumer<String, ShaderType, String> get_canpipe_onCompilationError();

    @Accessor("canpipe_onCompilationError")
    void set_canpipe_onCompilationError(TriConsumer<String, ShaderType, String> onCompilationError);

}
