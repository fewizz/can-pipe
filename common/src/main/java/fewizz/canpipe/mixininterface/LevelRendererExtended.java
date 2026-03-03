package fewizz.canpipe.mixininterface;

import org.joml.Matrix4f;

import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;

public interface LevelRendererExtended {

    boolean canpipe_getIsRenderingShadows();
    int canpipe_getShadowCascade();
    float canpipe_getEyeBlockLight();
    float canpipe_getEyeSkyLight();
    float canpipe_getSmoothedEyeBlockLight();
    float canpipe_getSmoothedEyeSkyLight();
    float canpipe_getSmoothedRainGradient();
    float canpipe_getSmoothedThunderGradient();

    int canpipe_getOriginType();
    void canpipe_setOriginType(int originType);

    void canpipe_prepareCascadesChunkSectionsToRender(Matrix4f viewMatrix, ChunkSectionsToRender[] chunkSectionsToRender);

}
