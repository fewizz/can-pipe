package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

import fewizz.canpipe.helpers.ShadowFrustum;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.fog.FogRenderer;

public interface GameRendererExtended {

    Matrix4f canpipe_worldViewMatrix();
    Matrix4f canpipe_worldProjectionMatrix();

    ShadowFrustum[] canpipe_getShadowFrustums();

    void canpipe_onPipelineActivated();

    FogRenderer canpipe_getFogRenderer();
    Lightmap canpipe_getLightmap();

    int canpipe_getOriginType();

    boolean canpipe_isRenderingHand();

}