package fewizz.canpipe.compat.indigo.mixininterface;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface QuadViewExtended /* extends QuadView */ {

    float canpipe_getAO(int index);
    TextureAtlasSprite canpipe_getSprite();

}
