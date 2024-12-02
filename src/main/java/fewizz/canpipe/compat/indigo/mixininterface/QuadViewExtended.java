package fewizz.canpipe.compat.indigo.mixininterface;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;

public interface QuadViewExtended extends QuadView {

    float getAO(int index);

    int getSpriteIndex();

    int getTangent();

}
