package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public interface LevelRendererExtended {

    boolean canpipe_getIsRenderingShadows();

    Matrix4f canpipe_getShadowViewMatrix();
    Matrix4f[] canpipe_getShadowProjectionMatrices();
    Vector4f[] canpipe_getShadowCenters();

}
