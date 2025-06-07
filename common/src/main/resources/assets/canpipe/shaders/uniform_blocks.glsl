layout(std140) uniform mc_ub_dynamic_transforms {
    mat4 mc_modelViewMatrix;
    vec4 mc_colorModulator;
    vec3 mc_modelToCamera;  // chunk block pos - camera pos when rendering chunks, vec3(0.0) otherwise
    mat4 mc_textureMat;
    float mc_lineWidth;
};

layout(std140) uniform mc_ub_projection {
    mat4 mc_projectionMatrix;
};

layout(std140) uniform mc_ub_fog {
    vec4 mc_fogColor;
    float mc_fogEnvironmentalStart;
    float mc_fogEnvironmentalEnd;
    float mc_fogRenderDistanceStart;
    float mc_fogRenderDistanceEnd;
    float mc_fogSkyEnd;
    float mc_fogCloudsEnd;
};

#ifdef CANPIPE_MATERIAL_SHADER
    layout(std140) uniform canpipe_ub_material_program {
        uniform int frxu_cascade;
        uniform int canpipe_renderTarget;
        uniform int canpipe_originType;
    };
#endif