#ifdef CANPIPE_TERRAIN
    #include minecraft:shaders/include/chunksection.glsl
    #include minecraft:shaders/include/globals.glsl
#else
    #include minecraft:shaders/include/dynamictransforms.glsl
#endif

#include minecraft:shaders/include/projection.glsl

layout(std140) uniform frx_ub_view {
    // mat4 frx_viewMatrix;  provided by DynamicTransforms and ChunkSection
    mat4 frx_inverseViewMatrix;
    mat4 frx_lastViewMatrix;

    // mat4 frx_projectionMatrix;  provided by mc_projectionMatrix
    mat4 frx_inverseProjectionMatrix;
    mat4 frx_lastProjectionMatrix;

    mat4 frx_cleanViewProjectionMatrix;
    mat4 frx_inverseCleanViewProjectionMatrix;

    // chunk block pos when frx_modelOriginRegion is true, camera pos when frx_modelOriginCamera is true, vec3(0.0) otherwise
    vec4 frx_modelToWorld;
    // vec4 frx_modelToCamera; provided by DynamicTransforms

    vec2 canpipe_screenSize;  // aka ScreenSize
    float frx_viewBrightness;
    float frx_viewDistance;
    int canpipe_viewFlags;

    vec3 frx_cameraView;
    vec3 frx_entityView;
    vec3 frx_cameraPos;
    vec3 frx_lastCameraPos;
};

layout(std140) uniform frx_ub_shadow {
    mat4 frx_shadowViewMatrix;
    mat4 frx_inverseShadowViewMatrix;
    vec4[4] canpipe_shadowCenters;
};

#define frx_viewMatrix ModelViewMat
#define frx_projectionMatrix ProjMat
#ifdef CANPIPE_TERRAIN
    #define frx_modelToCamera vec4(-CameraBlockPos + CameraOffset, 0.0)
#else
    #define frx_modelToCamera vec4(ModelOffset, 0.0)
#endif

#define MODEL_ORIGIN_CAMERA 0
#define MODEL_ORIGIN_REGION 1
#define MODEL_ORIGIN_SCREEN 2

#define frx_modelOriginCamera (frx_modelOriginType() == MODEL_ORIGIN_CAMERA)
#define frx_modelOriginRegion (frx_modelOriginType() == MODEL_ORIGIN_REGION)
#define frx_modelOriginScreen (frx_modelOriginType() == MODEL_ORIGIN_SCREEN)

int frx_modelOriginType() {
    #if defined CANPIPE_MATERIAL_SHADER
        #if defined CANPIPE_TERRAIN
            return MODEL_ORIGIN_REGION;
        #else
            return canpipe_originType;  // defined in canpipe_ub_origin_type
        #endif
    #else
        return MODEL_ORIGIN_SCREEN;  // always 2 (screen) for passes
    #endif
}

#if !defined CANPIPE_MATERIAL_SHADER || defined CANPIPE_TERRAIN
    #define frx_isHand false
#else
    #define frx_isHand (canpipe_isRenderingHand == 1)
#endif

#if defined CANPIPE_TERRAIN
    #define frx_isGui false
#else
    #define frx_isGui frx_modelOriginScreen
#endif

#define frx_guiViewProjectionMatrix frx_viewProjectionMatrix
#define frx_normalModelMatrix mat3(frx_viewMatrix)

#define frx_viewProjectionMatrix (frx_projectionMatrix*frx_viewMatrix)
#define frx_inverseViewProjectionMatrix (frx_inverseViewMatrix*frx_inverseProjectionMatrix)
#define frx_lastViewProjectionMatrix (frx_lastProjectionMatrix*frx_lastViewMatrix)

vec4 frx_shadowCenter(int index) {
    return canpipe_shadowCenters[index];
}

mat4 frx_shadowProjectionMatrix(int index) {
    vec4 center = frx_shadowCenter(index);
    float radius = center.w;
    #if defined CANPIPE_Z_ZERO_TO_ONE
        return transpose(mat4(
            1.0/radius, 0.0,         0.0,                      -center.x/radius,
            0.0,        1.0/radius,  0.0,                      -center.y/radius,
            0.0,        0.0,        -1.0/(-center.z + radius),  0.0,
            0.0,        0.0,         0.0,                       1.0
        ));
    #else
        return transpose(mat4(
            1.0/radius, 0.0,         0.0,                      -center.x/radius,
            0.0,        1.0/radius,  0.0,                      -center.y/radius,
            0.0,        0.0,        -2.0/(-center.z + radius), -1.0,
            0.0,        0.0,         0.0,                       1.0
        ));
    #endif
}

#define frx_shadowViewProjectionMatrix(index) (frx_shadowProjectionMatrix(index)*frx_shadowViewMatrix)

#define frx_viewWidth (canpipe_screenSize.x)
#define frx_viewHeight (canpipe_screenSize.y)

#define frx_cameraInFluid ((canpipe_viewFlags >> 0) & 1)
#define frx_cameraInWater ((canpipe_viewFlags >> 1) & 1)
#define frx_cameraInLava  ((canpipe_viewFlags >> 2) & 1)
#define frx_cameraInSnow  ((canpipe_viewFlags >> 3) & 1)

#if defined CANPIPE_MATERIAL_SHADER
    #define frx_renderTargetSolid       (canpipe_renderTarget == 0)
    #define frx_renderTargetTranslucent (canpipe_renderTarget == 1)
    #define frx_renderTargetEntity      (canpipe_renderTarget == 2)
    #define frx_renderTargetParticles   (canpipe_renderTarget == 3)
#endif