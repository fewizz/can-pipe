package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

import fewizz.canpipe.helpers.ShadowFrustum;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.fog.FogRenderer;

public interface GameRendererExtended {

    public Matrix4f canpipe_worldViewMatrix();
    public Matrix4f canpipe_worldProjectionMatrix();

    public ShadowFrustum[] canpipe_getShadowFrustums();

    public ChunkSectionsToRender[] canpipe_getChunkSectionsToRender();

    public void canpipe_onPipelineActivated();

    public FogRenderer canpipe_getFogRenderer();

    public int canpipe_getRenderTarget();
    public void canpipe_setRenderTarget(int renderTarget);

}