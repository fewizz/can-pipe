package fewizz.canpipe.compat.indigo.mixininterface;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

public interface MutableQuadViewImplExtended extends QuadEmitter {

    float getAO(int index);
    void setAO(int index, float value);

    void setSpriteIndex(int index);
    int getSpriteIndex();

    int getTangent();

    void computeTangent();

}
