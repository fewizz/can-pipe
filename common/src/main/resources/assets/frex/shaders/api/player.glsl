const float frx_effectModifier = 0.0;
const float frx_darknessEffectFactor = 0.0;
uniform vec4 frx_heldLight;
uniform float frx_heldLightInnerRadius;
uniform float frx_heldLightOuterRadius;

const int frx_effectSpeed = 0; // TODO define
const int frx_effectSlowness = 0; // TODO define
const int frx_effectHast = 0; // TODO define
const int frx_effectMiningFatigue = 0; // TODO define
const int frx_effectStrength = 0; // TODO define
const int frx_effectInstantHealth = 0; // TODO define
const int frx_effectInstantDamage = 0; // TODO define
const int frx_effectJumpBoost = 0; // TODO define
const int frx_effectNausea = 0; // TODO define
const int frx_effectRegeneration = 0; // TODO define
const int frx_effectResistance = 0; // TODO define
const int frx_effectFireResistance = 0; // TODO define
const int frx_effectWaterBreathing = 0; // TODO define
const int frx_effectInvisibility = 0; // TODO define
const int frx_effectBlindness = 0; // TODO define
const int frx_effectNightVision = 0; // TODO define
const int frx_effectHunger = 0; // TODO define
const int frx_effectWeakness = 0; // TODO define
const int frx_effectPoison = 0; // TODO define
const int frx_effectWither = 0; // TODO define
const int frx_effectHealthBoost = 0; // TODO define
const int frx_effectAbsorption = 0; // TODO define
const int frx_effectSaturation = 0; // TODO define
const int frx_effectGlowing = 0; // TODO define
const int frx_effectLevitation = 0; // TODO define
const int frx_effectLuck = 0; // TODO define
const int frx_effectUnluck = 0; // TODO define
const int frx_effectSlowFalling = 0; // TODO define
const int frx_effectConduitPower = 0; // TODO define
const int frx_effectDolphinsGrace = 0; // TODO define
const int frx_effectBadOmen = 0; // TODO define
const int frx_effectHeroOfTheVillage = 0; // TODO define
const int frx_effectDarkness = 0; // TODO define

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

const float frx_playerMood = 1.0;
uniform vec3 frx_eyePos;
uniform vec2 frx_eyeBrightness;
// TODO
#define frx_smoothedEyeBrightness frx_eyeBrightness