package fewizz.canpipe.mixininterface;

import java.util.Map;

import com.mojang.blaze3d.textures.GpuTexture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public interface TextureAtlasExtended {

    GpuTexture canpipe_getSpriteData();
    Map<ResourceLocation, TextureAtlasSprite> canpipe_getSprites();

}