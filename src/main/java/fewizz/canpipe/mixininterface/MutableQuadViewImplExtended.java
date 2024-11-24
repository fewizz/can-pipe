package fewizz.canpipe.mixininterface;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;

public interface MutableQuadViewImplExtended extends QuadEmitter {

    void setSpriteIndex(int index);
    int getSpriteIndex();
    int getTangent();

    void computeTangent();

}
