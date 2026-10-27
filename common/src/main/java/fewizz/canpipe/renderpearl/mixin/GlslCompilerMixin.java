package fewizz.canpipe.renderpearl.mixin;

import org.lwjgl.util.shaderc.Shaderc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;

import fewizz.canpipe.renderpearl.GlslCompilerExtended;

@Mixin(GlslCompiler.class)
public abstract class GlslCompilerMixin implements GlslCompilerExtended {

    @Unique private final ThreadLocal<Boolean> canpipe_enableAutoMapLocations = ThreadLocal.withInitial(() -> false);

    @Override
    public void canpipe_enableAutoMapLocationsForPendingCompilation() {
        this.canpipe_enableAutoMapLocations.set(true);
    }

    @ModifyReturnValue(method = "createBaseShaderOptions", at = @At("RETURN"))
    long enableAutoMapping(long shaderOptionsHandle) {
        if (this.canpipe_enableAutoMapLocations.get()) {
            Shaderc.shaderc_compile_options_set_auto_map_locations(shaderOptionsHandle, true);
            this.canpipe_enableAutoMapLocations.set(false);
        }
        return shaderOptionsHandle;
    }

}
