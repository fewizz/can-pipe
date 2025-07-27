package fewizz.canpipe.mixininterface;

import java.util.Map;

import com.mojang.blaze3d.buffers.GpuBuffer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public interface TextureAtlasExtended {

    GpuBuffer canpipe_getSpritesExtentsBuffer();
    Map<ResourceLocation, TextureAtlasSprite> canpipe_getSprites();

}