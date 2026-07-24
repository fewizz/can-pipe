package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.opengl.GlShaderModule;

import fewizz.canpipe.b3d.GpuShaderModule;

@Mixin(GlShaderModule.class)
public class GlShaderModuleMixin implements GpuShaderModule {

    @Override
    public boolean canpipe_isGettingOutputVariablesNamesSupported() {
        return false;
    }

    @Override
    public List<String> canpipe_getOutputVariablesNames() {
        throw new UnsupportedOperationException();
    }

}
