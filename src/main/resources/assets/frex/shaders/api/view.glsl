uniform vec3 frx_cameraView;  // TODO define
const vec3 frx_entityView = vec3(0.0);  // TODO define
uniform vec3 frx_cameraPos;
uniform vec3 frx_lastCameraPos;

uniform vec4 frx_modelToWorld;
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
#define frx_inverseViewMatrix inverse(frx_viewMatrix)
uniform mat4 frx_lastViewMatrix;

uniform mat4 frx_projectionMatrix;  // aka ProjMat
#define frx_inverseProjectionMatrix inverse(frx_projectionMatrix)
uniform mat4 frx_lastProjectionMatrix;

#define frx_viewProjectionMatrix (frx_projectionMatrix*frx_viewMatrix)
#define frx_inverseViewProjectionMatrix (frx_inverseViewMatrix*frx_inverseProjectionMatrix)
#define frx_lastViewProjectionMatrix (frx_lastProjectionMatrix*frx_lastViewMatrix)

const mat4 frx_cleanViewProjectionMatrix = mat4(1.0);  // TODO
const mat4 frx_inverseCleanViewProjectionMatrix = mat4(1.0);  // TODO

uniform mat4 frx_shadowViewMatrix;
#define frx_inverseShadowViewMatrix inverse(frx_shadowViewMatrix)

uniform mat4 canpipe_shadowProjectionMatrix_0;
uniform mat4 canpipe_shadowProjectionMatrix_1;
uniform mat4 canpipe_shadowProjectionMatrix_2;
uniform mat4 canpipe_shadowProjectionMatrix_3;

uniform vec4 canpipe_shadowCenter_0;
uniform vec4 canpipe_shadowCenter_1;
uniform vec4 canpipe_shadowCenter_2;
uniform vec4 canpipe_shadowCenter_3;

mat4 frx_shadowProjectionMatrix(int index) {
    if (index == 0) { return canpipe_shadowProjectionMatrix_0; }
    if (index == 1) { return canpipe_shadowProjectionMatrix_1; }
    if (index == 2) { return canpipe_shadowProjectionMatrix_2; }
    return canpipe_shadowProjectionMatrix_3;
}

vec4 frx_shadowCenter(int index) {
    if (index == 0) { return canpipe_shadowCenter_0; }
    if (index == 1) { return canpipe_shadowCenter_1; }
    if (index == 2) { return canpipe_shadowCenter_2; }
    return canpipe_shadowCenter_3;
}

#define frx_shadowViewProjectionMatrix(index) (frx_shadowProjectionMatrix(index)*frx_shadowViewMatrix)

uniform vec2 canpipe_screenSize;  // aka ScreenSize
#define frx_viewWidth (canpipe_screenSize.x)
#define frx_viewHeight (canpipe_screenSize.y)
const float frx_viewBrightness = 1.0; // TODO

uniform float frx_viewDistance;

const int frx_cameraInFluid = 0;  // TODO
const int frx_cameraInWater = 0;  // TODO
const int frx_cameraInLava = 0;  // TODO
const int frx_cameraInSnow = 0;  // TODO

#if defined CANPIPE_MATERIAL_SHADER
    #define frx_renderTargetSolid       (canpipe_renderTarget == 0)
    #define frx_renderTargetTranslucent (canpipe_renderTarget == 1)
    #define frx_renderTargetEntity      (canpipe_renderTarget == 2)
    #define frx_renderTargetParticles   (canpipe_renderTarget == 3)
#endif