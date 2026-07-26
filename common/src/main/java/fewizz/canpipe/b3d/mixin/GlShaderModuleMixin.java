package fewizz.canpipe.b3d.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.opengl.GlShaderModule;

import fewizz.canpipe.b3d.GpuShaderModule;

@Mixin(GlShaderModule.class)
public class GlShaderModuleMixin implements GpuShaderModule {}
