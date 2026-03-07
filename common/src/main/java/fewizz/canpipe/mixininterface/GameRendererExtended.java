package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

import fewizz.canpipe.helpers.ShadowFrustum;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.fog.FogRenderer;

public interface GameRendererExtended {

    public Matrix4f canpipe_worldViewMatrix();
    public Matrix4f canpipe_worldProjectionMatrix();

    public ShadowFrustum[] canpipe_getShadowFrustums();

    public void canpipe_onPipelineActivated();

    public FogRenderer canpipe_getFogRenderer();
    public Lightmap canpipe_getLightmap();

    public int canpipe_getRenderTarget();
    public void canpipe_setRenderTarget(int renderTarget);

}