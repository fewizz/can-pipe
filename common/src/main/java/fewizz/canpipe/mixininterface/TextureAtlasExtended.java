package fewizz.canpipe.mixininterface;

import java.util.Map;

import com.mojang.blaze3d.textures.GpuTextureView;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public interface TextureAtlasExtended {

    GpuTextureView canpipe_getSpriteData();
    Map<ResourceLocation, TextureAtlasSprite> canpipe_getSprites();

}