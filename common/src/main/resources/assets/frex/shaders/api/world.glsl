// world.getGameTime() - ticks, ignoring doDaylightCycle gamerule
// world.getDayTime() - ticks, respecting doDaylightCycle gamerule (i.e., it won't be incremented if rule is enabled)
// world.getTimeOfDay() - used for sun location calculations; [0.0-1.0]; for vanilla overworld: 0.0 - noon, 0.5 - midnight, https://www.desmos.com/calculator/6haoppz00u

uniform int canpipe_renderFrames;
uniform float canpipe_fixedOrDayTime;  // (dimensionType.fixedTime() or else world.getDayTime() % 24000) / 24000.0; [0.0-1.0]; 13.0/24.0 - night, 23.0/24.0 - sunrise

uniform float frx_renderSeconds;
#define frx_renderFrames uint(canpipe_renderFrames)
uniform float frx_worldDay;   // (world.getDayTime() / 24000) % 2147483647L (why float?)
uniform float frx_worldTime;  // (world.getDayTime() % 24000) / 24000.0; [0.0-1.0]
const float frx_moonSize = 1.0;  // TODO

uniform float frx_skyAngleRadians;  // world.getTimeOfDay() * 2PI for vanilla
uniform vec3 frx_skyLightVector;  // points to the sun or moon
#define frx_skyLightColor (frx_worldIsMoonlit ? vec3(1.0, 0.5475, 0.5475) : vec3(1.0))  // unhardcode?
#define frx_skyLightIlluminance (frx_worldIsMoonlit ? 2000.0 : 32000.0)  // unhardcode?
const vec3 frx_skyLightAtmosphericColor = vec3(1.0);  // TODO
#define frx_skyLightTransitionFactor min(1.0, min(abs(canpipe_fixedOrDayTime*24.0-13.0), abs(canpipe_fixedOrDayTime*24-23.0)))  /*https://www.desmos.com/calculator/a6ouxizdbp*/
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
#define frx_worldIsMoonlit float(canpipe_fixedOrDayTime > 13.0/24.0 && canpipe_fixedOrDayTime < 23.0/24.0)