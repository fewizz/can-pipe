uniform float frx_renderSeconds;  // aka GameTime
uniform int canpipe_renderFrames;
#define frx_renderFrames uint(canpipe_renderFrames)
const float frx_worldDay = 0.0;  // TODO
const float frx_worldTime = 0.0;  // TODO
const float frx_moonSize = 1.0;  // TODO

const float frx_skyAngleRadians = 0.0;  // TODO
const vec3 frx_skyLightVector = vec3(0.0, 1.0, 0.0);  // TODO
const vec3 frx_skyLightColor = vec3(1.0);  // TODO
const float frx_skyLightIlluminance = 1.0;  // TOOD
const vec3 frx_skyLightAtmosphericColor = vec3(1.0);  // TOOD
const float frx_skyLightTransitionFactor = 1.0;  // TODO
const float frx_skyFlashStrength = 0.0;  // TODO

const float frx_ambientIntensity = 1.0;  // TODO
const vec4 frx_emissiveColor = vec4(1.0);  // TODO
const float frx_rainGradient = 0.0;  // TODO
const float frx_thunderGradient = 0.0;  // TODO
const float frx_smoothedRainGradient = 0.0;  // TODO
const float frx_smoothedThunderGradient = 0.0;  // TODO
const vec3 frx_vanillaClearColor = vec3(0.0);  // TODO
const int frx_worldHasSkylight = 1;  // TODO
const int frx_worldIsOverworld = 1;  // TODO
const int frx_worldIsNether = 0;  // TODO
const int frx_worldIsEnd = 0;  // TODO
const int frx_worldIsRaining = 0;  // TODO
const int frx_worldIsThundering = 0;  // TODO
const int frx_worldIsSkyDarkened = 0;  // TODO
const int frx_worldIsMoonlit = 0;  // TODO
