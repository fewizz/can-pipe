#include minecraft:shaders/include/fog.glsl

layout(std140) uniform frx_ub_fog {
    vec4 frx_fogColor;
    float frx_fogStart;
    float frx_fogEnd;
};

#define frx_fogEnabled 1