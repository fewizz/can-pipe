package fewizz.canpipe.compat.indigo;

import fewizz.canpipe.material.MaterialMap;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface MutableQuadViewExtended extends MutableQuadView, QuadViewExtended {

    void canpipe_setAO(int index, float value);
    void canpipe_setSprite(TextureAtlasSprite sprite);
    void canpipe_setMaterialMap(MaterialMap materialMap);

}
