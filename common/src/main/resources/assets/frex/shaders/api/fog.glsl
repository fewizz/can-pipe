#include minecraft:shaders/include/fog.glsl

layout(std140) uniform frx_ub_fog {
    vec4 frx_fogColor;
    // float frx_fogStart;
    // float frx_fogEnd;
    int frx_fogEnabled;
};

#define frx_fogStart FogRenderDistanceStart
#define frx_fogEnd FogRenderDistanceEnd
