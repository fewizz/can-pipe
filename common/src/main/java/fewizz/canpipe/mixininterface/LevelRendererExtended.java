package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public interface LevelRendererExtended {

    boolean getIsRenderingShadow();
    void setIsRenderingShadow(boolean v);

    Matrix4f getShadowViewMatrix();
    Matrix4f[] getShadowProjectionMatrices();
    Vector4f[] getShadowCenters();

}
