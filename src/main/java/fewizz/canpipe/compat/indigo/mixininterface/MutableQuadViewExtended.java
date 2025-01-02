package fewizz.canpipe.compat.indigo.mixininterface;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface MutableQuadViewExtended extends MutableQuadView, QuadViewExtended {

    void canpipe_setAO(int index, float value);
    void canpipe_setSprite(TextureAtlasSprite sprite);

}
