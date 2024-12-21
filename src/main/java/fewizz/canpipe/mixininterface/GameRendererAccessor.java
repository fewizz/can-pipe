package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface GameRendererAccessor {

    public int canpipe_getFrame();
    public float canpipe_getRenderSeconds();

    public Vector3f canpipe_getLastCameraPos();
    public Matrix4f canpipe_getLastViewMatrix();
    public Matrix4f canpipe_getLastProjectionMatrix();

    // public Matrix4f canpipe_getShadowViewMatrix();
    // public Matrix4f canpipe_getShadowProjectionMatrix();

    public void canpipe_onPipelineActivated();

}