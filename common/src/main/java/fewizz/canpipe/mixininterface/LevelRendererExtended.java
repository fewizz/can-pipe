package fewizz.canpipe.mixininterface;

public interface LevelRendererExtended {

    int canpipe_getCurrentShadowCascadeIdx();
    float canpipe_getEyeBlockLight();
    float canpipe_getEyeSkyLight();
    float canpipe_getSmoothedEyeBlockLight();
    float canpipe_getSmoothedEyeSkyLight();
    float canpipe_getSmoothedRainGradient();
    float canpipe_getSmoothedThunderGradient();

}
