const vec3 frx_cameraView = vec3(0.0);  // TODO define
const vec3 frx_entityView = vec3(0.0);  // TODO define
uniform vec3 frx_cameraPos;
uniform vec3 frx_lastCameraPos;

const vec4 frx_modelToWorld = vec4(0.0);  // TODO define
uniform vec3 canpipe_modelToCamera;  // aka ChunkOffset
#define frx_modelToCamera vec4(canpipe_modelToCamera, 0.0)
const bool frx_modelOriginScreen = false;  // TODO define
const bool frx_modelOriginRegion = false;  // TODO define

const bool frx_isHand = false; // TODO define
const bool frx_isGui = frx_modelOriginScreen;

const mat4 frx_guiViewProjectionMatrix = mat4(1.0);  // TODO define
const mat3 frx_normalModelMatrix = mat3(1.0);  // TODO define

uniform mat4 frx_viewMatrix;  // aka ModelViewMat
#define frx_inverseViewMatrix inverse(frx_viewMatrix)
uniform mat4 frx_lastViewMatrix;

uniform mat4 frx_projectionMatrix;  // aka ProjMat
#define frx_inverseProjectionMatrix inverse(frx_projectionMatrix)
uniform mat4 frx_lastProjectionMatrix;

#define frx_viewProjectionMatrix (frx_projectionMatrix * frx_viewMatrix)
#define frx_inverseViewProjectionMatrix inverse(frx_viewProjectionMatrix)
#define frx_lastViewProjectionMatrix (frx_lastProjectionMatrix*frx_lastViewMatrix)

const mat4 frx_cleanViewProjectionMatrix = mat4(1.0);  // TODO define
const mat4 frx_inverseCleanViewProjectionMatrix = mat4(1.0);  // TODO define

#define frx_shadowViewMatrix mat4(1.0)  // TODO define
#define frx_shadowProjectionMatrix(index) mat4(1.0)  // TODO define
#define frx_shadowViewProjectionMatrix(index) mat4(1.0)  // TODO define
#define frx_shadowCenter(index) vec4(0.0)  // TODO define

uniform vec2 canpipe_screenSize;  // aka ScreenSize
#define frx_viewWidth (canpipe_screenSize.x)
#define frx_viewHeight (canpipe_screenSize.y)

const float frx_viewDistance = 4.0F*16.0F;  // TODO define

const int frx_cameraInFluid = 0;  // TODO define
const int frx_cameraInWater = 0;  // TODO define
const int frx_cameraInLava = 0;  // TODO define
const int frx_cameraInSnow = 0;  // TODO define