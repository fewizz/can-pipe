#include canpipe:shaders/uniform_blocks.glsl

layout(std140) uniform frx_ub_fog {
    uniform vec4 frx_fogColor;
    // uniform float frx_fogStart;
    // uniform float frx_fogEnd;
    uniform int frx_fogEnabled;
};

#define frx_fogStart mc_fogRenderDistanceStart
#define frx_fogEnd mc_fogRenderDistanceEnd
