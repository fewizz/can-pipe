#version 150

uniform sampler2D InSampler;

void main() {
    gl_FragDepth = texelFetch(InSampler, ivec2(gl_FragCoord.xy), 0).r;
}