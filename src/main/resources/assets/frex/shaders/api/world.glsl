uniform int canpipe_renderFrames;
uniform float canpipe_timeOfDay;  // aka world.getTimeOfDay(); [0.0-1.0]; 0.0 - noon, 0.5 - midnight

uniform float frx_renderSeconds;
#define frx_renderFrames uint(canpipe_renderFrames)
uniform float frx_worldDay;  // (aka world.getDayTime() / 24000) % 2147483647L (why float tho?)
uniform float frx_worldTime;  // aka (world.getDayTime() % 24000.0) / 24000.0
const float frx_moonSize = 1.0;  // TODO

const float frx_skyAngleRadians = 0.0;  // TODO
uniform vec3 frx_skyLightVector;
const vec3 frx_skyLightColor = vec3(1.0);  // TODO
const float frx_skyLightIlluminance = 1.0;  // TOOD
const vec3 frx_skyLightAtmosphericColor = vec3(1.0);  // TOOD
#define frx_skyLightTransitionFactor canpipe_skyLightTransitionFactor()  // aka world.getTimeOfDay()
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


float canpipe_skyLightTransitionFactor() {
    /*float t = canpipe_timeOfDay * 24.0;
    float f = 1.0;

    if (t > 22.0) {
        f = abs(23.0 - t);
    } else if (t > 12.0) {
        f = abs(13.0 - t);
    }

    return f;*/
    return 1.0;  // TODO
}