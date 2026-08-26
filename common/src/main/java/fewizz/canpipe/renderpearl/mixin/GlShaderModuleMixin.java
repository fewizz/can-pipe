package fewizz.canpipe.renderpearl.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.renderpearl.backend.opengl.GlShaderModule;

import fewizz.canpipe.renderpearl.GpuShaderModule;

@Mixin(GlShaderModule.class)
public class GlShaderModuleMixin implements GpuShaderModule {}
