uniform float frx_effectModifier;

uniform float canpipe_darknessFactor;
#define frx_darknessEffectFactor canpipe_darknessFactor  // because Lumi checks for definition (header.glsl)

uniform vec3 frx_eyePos;

uniform vec2 frx_eyeBrightness;
uniform vec2 frx_smoothedEyeBrightness;

uniform vec4 frx_heldLight;
uniform float frx_heldLightInnerRadius;
uniform float frx_heldLightOuterRadius;

uniform float frx_playerMood;

uniform int canpipe_playerFlags;
#define frx_playerEyeInFluid   ((canpipe_playerFlags >>  0) & 1)
#define frx_playerEyeInWater   ((canpipe_playerFlags >>  1) & 1)
#define frx_playerEyeInLava    ((canpipe_playerFlags >>  2) & 1)
#define frx_playerSneaking     ((canpipe_playerFlags >>  3) & 1)
#define frx_playerSwimming     ((canpipe_playerFlags >>  4) & 1)
#define frx_playerSneakingPose ((canpipe_playerFlags >>  5) & 1)
#define frx_playerSwimmingPose ((canpipe_playerFlags >>  6) & 1)
#define frx_playerCreative     ((canpipe_playerFlags >>  7) & 1)
#define frx_playerSpectator    ((canpipe_playerFlags >>  8) & 1)
#define frx_playerRiding       ((canpipe_playerFlags >>  9) & 1)
#define frx_playerOnFire       ((canpipe_playerFlags >> 10) & 1)
#define frx_playerSleeping     ((canpipe_playerFlags >> 11) & 1)
#define frx_playerSprinting    ((canpipe_playerFlags >> 12) & 1)
#define frx_playerWet          ((canpipe_playerFlags >> 13) & 1)
#define frx_playerEyeInSnow    ((canpipe_playerFlags >> 14) & 1)
#define frx_playerIsFreezing   ((canpipe_playerFlags >> 15) & 1)

uniform ivec2 canpipe_effectsFlags;
#define frx_effectSpeed            ((canpipe_effectsFlags[0] >>  0) & 1)
#define frx_effectSlowness         ((canpipe_effectsFlags[0] >>  1) & 1)
#define frx_effectHast             ((canpipe_effectsFlags[0] >>  2) & 1)
#define frx_effectMiningFatigue    ((canpipe_effectsFlags[0] >>  3) & 1)
#define frx_effectStrength         ((canpipe_effectsFlags[0] >>  4) & 1)
#define frx_effectInstantHealth    ((canpipe_effectsFlags[0] >>  5) & 1)
#define frx_effectInstantDamage    ((canpipe_effectsFlags[0] >>  6) & 1)
#define frx_effectJumpBoost        ((canpipe_effectsFlags[0] >>  7) & 1)
#define frx_effectNausea           ((canpipe_effectsFlags[0] >>  8) & 1)
#define frx_effectRegeneration     ((canpipe_effectsFlags[0] >>  9) & 1)
#define frx_effectResistance       ((canpipe_effectsFlags[0] >> 10) & 1)
#define frx_effectFireResistance   ((canpipe_effectsFlags[0] >> 11) & 1)
#define frx_effectWaterBreathing   ((canpipe_effectsFlags[0] >> 12) & 1)
#define frx_effectInvisibility     ((canpipe_effectsFlags[0] >> 13) & 1)
#define frx_effectBlindness        ((canpipe_effectsFlags[0] >> 14) & 1)
#define frx_effectNightVision      ((canpipe_effectsFlags[0] >> 15) & 1)
#define frx_effectHunger           ((canpipe_effectsFlags[0] >> 16) & 1)
#define frx_effectWeakness         ((canpipe_effectsFlags[0] >> 17) & 1)
#define frx_effectPoison           ((canpipe_effectsFlags[0] >> 18) & 1)
#define frx_effectWither           ((canpipe_effectsFlags[0] >> 19) & 1)
#define frx_effectHealthBoost      ((canpipe_effectsFlags[0] >> 20) & 1)
#define frx_effectAbsorption       ((canpipe_effectsFlags[0] >> 21) & 1)
#define frx_effectSaturation       ((canpipe_effectsFlags[0] >> 22) & 1)
#define frx_effectGlowing          ((canpipe_effectsFlags[0] >> 23) & 1)
#define frx_effectLevitation       ((canpipe_effectsFlags[0] >> 24) & 1)
#define frx_effectLuck             ((canpipe_effectsFlags[0] >> 25) & 1)
#define frx_effectUnluck           ((canpipe_effectsFlags[0] >> 26) & 1)
#define frx_effectSlowFalling      ((canpipe_effectsFlags[0] >> 27) & 1)
#define frx_effectConduitPower     ((canpipe_effectsFlags[0] >> 28) & 1)
#define frx_effectDolphinsGrace    ((canpipe_effectsFlags[0] >> 29) & 1)
#define frx_effectBadOmen          ((canpipe_effectsFlags[0] >> 30) & 1)
#define frx_effectHeroOfTheVillage ((canpipe_effectsFlags[0] >> 31) & 1)
#define frx_effectDarkness         ((canpipe_effectsFlags[1] >>  0) & 1)
// for the future