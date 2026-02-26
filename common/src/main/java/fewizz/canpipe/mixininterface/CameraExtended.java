package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

public interface CameraExtended {

    Matrix4f canpipe_createProjectionMatrixForCulling(float depthFar);

}
