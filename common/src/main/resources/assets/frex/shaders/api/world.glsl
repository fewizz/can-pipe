// world.getGameTime() - ticks, ignoring doDaylightCycle gamerule
// world.getDayTime() - ticks, respecting doDaylightCycle gamerule (i.e., it won't be incremented if rule is enabled)
// world.getTimeOfDay() - used for sun location calculations; [0.0-1.0]; for vanilla overworld: 0.0 - noon, 0.5 - midnight, https://www.desmos.com/calculator/6haoppz00u

layout(std140) uniform frx_ub_world {
    int canpipe_renderFrames;
    int canpipe_worldFlags;
    float canpipe_fixedOrDayTime;  // (dimensionType.fixedTime() or world.getDayTime() % 24000) / 24000.0; [0.0-1.0]; 13.0/24.0 - night, 23.0/24.0 - sunrise
    float frx_renderSeconds;
    float frx_worldDay;   // (world.getDayTime() / 24000) % 2147483647L (why float?)
    float frx_worldTime;  // (world.getDayTime() % 24000) / 24000.0; [0.0-1.0]
    float frx_moonSize;
    float frx_skyAngleRadians;  // world.getTimeOfDay() * 2PI for vanilla
    float frx_skyFlashStrength;
    float frx_ambientIntensity;
    vec4 frx_emissiveColor;
    vec4 canpipe_weatherGradients;
    vec3 frx_skyLightVector;  // points to the sun or moon
    vec3 canpipe_sunriseOrSunsetColor;  // vec3(1.0) if unavailable
    vec3 frx_vanillaClearColor;
};

#define frx_renderFrames uint(canpipe_renderFrames)

#define frx_skyLightColor (frx_worldIsMoonlit == 1.0 ? vec3(1.0, 0.5475, 0.5475) : vec3(1.0))  // unhardcode?
#define frx_skyLightIlluminance (frx_worldIsMoonlit == 1.0 ? 2000.0 : 32000.0)  // unhardcode?

#define frx_skyLightAtmosphericColor canpipe_sunriseOrSunsetColor

#define frx_skyLightTransitionFactor canpipe_getSkyLightTransitionFactor()

#define frx_rainGradient            (canpipe_weatherGradients.x)
#define frx_thunderGradient         (canpipe_weatherGradients.y)
#define frx_smoothedRainGradient    (canpipe_weatherGradients.z)
#define frx_smoothedThunderGradient (canpipe_weatherGradients.w)

#define frx_worldHasSkylight     ((canpipe_worldFlags >> 0) & 1)
#define frx_worldIsRaining       ((canpipe_worldFlags >> 1) & 1)
#define frx_worldIsThundering    ((canpipe_worldFlags >> 2) & 1)
#define frx_worldIsSkyDarkened   ((canpipe_worldFlags >> 3) & 1)
#define frx_worldIsOverworld int(((canpipe_worldFlags >> 4) & 3) == 0)
#define frx_worldIsNether    int(((canpipe_worldFlags >> 4) & 3) == 1)
#define frx_worldIsEnd       int(((canpipe_worldFlags >> 4) & 3) == 2)

#define frx_worldIsMoonlit float(frx_worldHasSkylight == 1 && canpipe_fixedOrDayTime > 13.0/24.0 && canpipe_fixedOrDayTime < 23.0/24.0)

float canpipe_getSkyLightTransitionFactor() {
    if (frx_worldHasSkylight == 1) {
        return min(
            1.0,
            min( /* https://www.desmos.com/calculator/a6ouxizdbp */
                abs(canpipe_fixedOrDayTime*24.0-13.0),
                abs(canpipe_fixedOrDayTime*24.0-23.0)
            )
        );
    }
    return 1.0;
}