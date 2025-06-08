const int frx_matEmissive = 0;  // TODO define
const int frx_matCutout = 0;  // TODO define
const int frx_matUnmipped = 0;  // TODO define
#define frx_matDisableAo ((canpipe_materialFlags >> 1) & 1)
#define frx_matDisableDiffuse ((canpipe_materialFlags >> 2) & 1)
#if defined CANPIPE_HAS_OVERLAY_POS
    #define frx_matHurt (canpipe_overlayPos.y == 3 ? 1 : 0)
    #define frx_matFlash (canpipe_overlayPos.y == 10 ? 1 : 0)
#else
    const int frx_matHurt = 0;
    const int frx_matFlash = 0;
#endif
#define frx_matGlint ((canpipe_materialFlags >> 0) & 1)
const int frx_matGlintEntity = 0;  // unused?
const float frx_matExposure = 0.0;  // unused?