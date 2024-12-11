const vec3 frx_cameraView = vec3(0.0);  // TODO define
const vec3 frx_entityView = vec3(0.0);  // TODO define
uniform vec3 frx_cameraPos;
uniform vec3 frx_lastCameraPos;

uniform vec4 frx_modelToWorld;
uniform vec3 canpipe_modelToCamera;
#define frx_modelToCamera vec4(canpipe_modelToCamera, 1.0)
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

#define frx_shadowViewMatrix mat4(1.0)  // TODO
#define frx_shadowProjectionMatrix(index) mat4(1.0)  // TODO
#define frx_shadowViewProjectionMatrix(index) mat4(1.0)  // TODO
#define frx_shadowCenter(index) vec4(0.0)  // TODO

uniform vec2 canpipe_screenSize;  // aka ScreenSize
#define frx_viewWidth (canpipe_screenSize.x)
#define frx_viewHeight (canpipe_screenSize.y)
const float frx_viewBrightness = 1.0; // TODO

uniform float frx_viewDistance;

const int frx_cameraInFluid = 0;  // TODO
const int frx_cameraInWater = 0;  // TODO
const int frx_cameraInLava = 0;  // TODO
const int frx_cameraInSnow = 0;  // TODO

int canpipe_renderTarget() {
    if (!(frx_isGui && !frx_isHand)) {
        #if defined _RENDERTYPE_TRANSLUCENT
            return 1;  // translucent
        #elif defined _RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL
            return 2;  // entity
        #elif defined _PARTICLES
            return 3;  // particle
        #endif
    }
    return 0;  // solid
}

#define frx_renderTargetSolid       (canpipe_renderTarget() == 0)
#define frx_renderTargetTranslucent (canpipe_renderTarget() == 1)
#define frx_renderTargetEntity      (canpipe_renderTarget() == 2)
#define frx_renderTargetParticles   (canpipe_renderTarget() == 3)