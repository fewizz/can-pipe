package fewizz.canpipe.mixininterface;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface TextureAtlasExtended {

    AbstractTexture getSpriteData();

    int indexBySprite(TextureAtlasSprite sprite);

}