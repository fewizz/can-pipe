#include minecraft:shaders/include/fog.glsl

layout(std140) uniform frx_ub_fog {
    uniform vec4 frx_fogColor;
    // uniform float frx_fogStart;
    // uniform float frx_fogEnd;
    uniform int frx_fogEnabled;
};

#define frx_fogStart FogRenderDistanceStart
#define frx_fogEnd FogRenderDistanceEnd
