package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

import net.minecraft.client.renderer.fog.FogRenderer;

public interface GameRendererExtended {

    public Matrix4f canpipe_worldViewMatrix();
    public Matrix4f canpipe_worldProjectionMatrix();

    public Matrix4f[] canpipe_getShadowProjectionMatrices();
    public Matrix4f[] canpipe_getShortenedViewProjectionMatrices();

    public void canpipe_onPipelineActivated();

    public FogRenderer canpipe_getFogRenderer();

}