uniform vec3 frx_cameraView;
const vec3 frx_entityView = vec3(0.0);  // TODO define
uniform vec3 frx_cameraPos;
uniform vec3 frx_lastCameraPos;

// chunk block pos when frx_modelOriginRegion is true, camera pos when frx_modelOriginCamera is true, vec3(0.0) otherwise
uniform vec4 frx_modelToWorld;
// chunk block pos - camera pos when rendering chunks, vec3(0.0) otherwise
uniform vec3 canpipe_modelToCamera;
#define frx_modelToCamera vec4(canpipe_modelToCamera, 0.0)
uniform int canpipe_originType;
#define frx_modelOriginCamera (canpipe_originType == 0)
#define frx_modelOriginRegion (canpipe_originType == 1)
#define frx_modelOriginScreen (canpipe_originType == 2 || canpipe_originType == 3)
#define frx_isHand (canpipe_originType == 3)
#define frx_isGui frx_modelOriginScreen

#define frx_guiViewProjectionMatrix frx_viewProjectionMatrix
#define frx_normalModelMatrix mat3(frx_viewMatrix)

uniform mat4 frx_viewMatrix;  // aka ModelViewMat
uniform mat4 frx_inverseViewMatrix;
uniform mat4 frx_lastViewMatrix;

uniform mat4 frx_projectionMatrix;  // aka ProjMat
uniform mat4 frx_inverseProjectionMatrix;
uniform mat4 frx_lastProjectionMatrix;

#define frx_viewProjectionMatrix (frx_projectionMatrix*frx_viewMatrix)
#define frx_inverseViewProjectionMatrix (frx_inverseViewMatrix*frx_inverseProjectionMatrix)
#define frx_lastViewProjectionMatrix (frx_lastProjectionMatrix*frx_lastViewMatrix)

const mat4 frx_cleanViewProjectionMatrix = mat4(1.0);  // TODO
const mat4 frx_inverseCleanViewProjectionMatrix = mat4(1.0);  // TODO

uniform mat4 frx_shadowViewMatrix;
uniform mat4 frx_inverseShadowViewMatrix;

uniform vec4 canpipe_shadowCenter_0;
uniform vec4 canpipe_shadowCenter_1;
uniform vec4 canpipe_shadowCenter_2;
uniform vec4 canpipe_shadowCenter_3;

vec4 frx_shadowCenter(int index) {
    if (index == 0) { return canpipe_shadowCenter_0; }
    if (index == 1) { return canpipe_shadowCenter_1; }
    if (index == 2) { return canpipe_shadowCenter_2; }
    return canpipe_shadowCenter_3;
}

mat4 frx_shadowProjectionMatrix(int index) {
    vec4 center = frx_shadowCenter(index);
    float radius = center.w;
    return transpose(mat4(
        1.0/radius, 0.0,         0.0,                      -center.x/radius,
        0.0,        1.0/radius,  0.0,                      -center.y/radius,
        0.0,        0.0,        -2.0/(-center.z + radius), -1.0,
        0.0,        0.0,         0.0,                       1.0
    ));
}

#define frx_shadowViewProjectionMatrix(index) (frx_shadowProjectionMatrix(index)*frx_shadowViewMatrix)

uniform vec2 canpipe_screenSize;  // aka ScreenSize
#define frx_viewWidth (canpipe_screenSize.x)
#define frx_viewHeight (canpipe_screenSize.y)
uniform float frx_viewBrightness;

uniform float frx_viewDistance;

uniform int canpipe_viewFlags;

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