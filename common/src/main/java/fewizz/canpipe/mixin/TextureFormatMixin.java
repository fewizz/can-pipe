package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.mojang.blaze3d.textures.TextureFormat;

@Mixin(TextureFormat.class)  // Mixin plugin adds IVEC2 uniform
public class TextureFormatMixin {}
