package fewizz.canpipe.compat.indigo;

import fewizz.canpipe.material.MaterialMap;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface QuadViewExtended extends QuadView {

    float canpipe_getAO(int index);
    TextureAtlasSprite canpipe_getSprite();
    MaterialMap canpipe_getMaterialMap();

}
