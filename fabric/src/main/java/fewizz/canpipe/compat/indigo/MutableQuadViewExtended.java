package fewizz.canpipe.compat.indigo;

import java.util.function.Function;

import fewizz.canpipe.material.Material;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface MutableQuadViewExtended extends MutableQuadView, QuadViewExtended {

    void canpipe_setAO(int index, float value);
    void canpipe_setSprite(TextureAtlasSprite sprite);
    void canpipe_setMaterialSupplier(Function<TextureAtlasSprite, Material> materialSupplier);

}
