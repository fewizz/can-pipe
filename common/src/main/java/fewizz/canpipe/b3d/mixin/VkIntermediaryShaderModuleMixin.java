package fewizz.canpipe.b3d.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import com.mojang.blaze3d.vulkan.glsl.SpvVariable;

import fewizz.canpipe.b3d.GpuShaderModule;

@Mixin(IntermediaryShaderModule.class)
public class VkIntermediaryShaderModuleMixin implements GpuShaderModule {

    @Shadow @Final private List<SpvVariable> outputs;

    @Override
    public boolean canpipe_isGettingOutputVariablesNamesSupported() {
        return true;
    }

    @Override
    public List<String> canpipe_getOutputVariablesNames() {
        return this.outputs.stream().map(x -> x.name()).toList();
    }

}
