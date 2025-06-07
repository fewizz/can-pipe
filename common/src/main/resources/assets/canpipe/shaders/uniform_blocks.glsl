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

#ifdef CANPIPE_MATERIAL_SHADER
    layout(std140) uniform canpipe_ub_material_program {
        uniform int frxu_cascade;
        uniform int canpipe_renderTarget;
        uniform int canpipe_originType;
        uniform vec3 canpipe_light0Direction;
        uniform vec3 canpipe_light1Direction;
    };
#endif