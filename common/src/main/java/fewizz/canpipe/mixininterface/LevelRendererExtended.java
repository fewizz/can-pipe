package fewizz.canpipe.mixininterface;

public interface LevelRendererExtended {

    boolean canpipe_getIsRenderingShadows();
    int canpipe_getShadowCascade();
    float canpipe_getEyeBlockLight();
    float canpipe_getEyeSkyLight();
    float canpipe_getSmoothedEyeBlockLight();
    float canpipe_getSmoothedEyeSkyLight();
    float canpipe_getSmoothedRainGradient();
    float canpipe_getSmoothedThunderGradient();

}
