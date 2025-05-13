package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.GameRenderer;

@Mixin(value = GameRenderer.class, priority = 1001)
public interface GameRendererExtended {

    public int canpipe_getFrame();
    public float canpipe_getRenderSeconds();

    public Vector3f canpipe_getLastCameraPos();
    public Matrix4f canpipe_getViewMatrix();
    public Matrix4f canpipe_getLastViewMatrix();
    public Matrix4f canpipe_getProjectionMatrix();
    public Matrix4f canpipe_getLastProjectionMatrix();

    public Matrix4f canpipe_getShadowViewMatrix();
    public Matrix4f[] canpipe_getShadowProjectionMatrices();
    public Matrix4f[] canpipe_getShortenedViewProjectionMatrices();
    public Vector4f[] canpipe_getShadowCenters();

    public void canpipe_onPipelineActivated();

}