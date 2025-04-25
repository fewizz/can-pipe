package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.textures.GpuTexture;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

@Mixin(GlProgram.class)
public interface GlProgramAccessor {
    
    @Accessor("samplerTextures")
    Object2ObjectMap<String, GpuTexture> canpipe_getSamplerTextures();

}
