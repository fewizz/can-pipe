package fewizz.canpipe.mixininterface;

import java.util.Map;

import com.mojang.blaze3d.buffers.GpuBuffer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public interface TextureAtlasExtended {

    GpuBuffer canpipe_getSpritesExtentsBuffer();
    Map<Identifier, TextureAtlasSprite> canpipe_getSprites();

}