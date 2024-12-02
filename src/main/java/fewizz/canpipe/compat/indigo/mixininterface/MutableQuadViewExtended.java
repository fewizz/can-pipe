package fewizz.canpipe.compat.indigo.mixininterface;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;

public interface MutableQuadViewExtended extends MutableQuadView, QuadViewExtended {

    void setAO(int index, float value);

    void setSpriteIndex(int index);

    void computeTangent();

}
