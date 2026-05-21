const int frx_matCutout = 0;  // TODO define
const int frx_matUnmipped = 0;  // TODO define
const float frx_matExposure = 0.0;  // unused?

#if defined CANPIPE_HAS_MATERIAL_FLAGS
    #define frx_matDisableAo ((canpipe_materialFlags >> 2) & 1)
    #define frx_matDisableDiffuse ((canpipe_materialFlags >> 3) & 1)
    #define canpipe_disableColorIndex ((canpipe_materialFlags >> 4) & 1)
    #define frx_matEmissive ((canpipe_materialFlags >> 5) & 1)
    #define frx_matGlint (((canpipe_materialFlags >> 0) & 1) | frx_matGlintEntity)
    #define frx_matGlintEntity ((canpipe_materialFlags >> 1) & 1)
#else
    #define frx_matDisableAo 0
    #define frx_matDisableDiffuse 0
    #define canpipe_disableColorIndex 0
    #define frx_matEmissive 0
    #define frx_matGlint 0
    #define frx_matGlintEntity 0
#endif

#if defined CANPIPE_HAS_OVERLAY_POS
    #define frx_matHurt (canpipe_overlayPos.y == 3 ? 1 : 0)
    #define frx_matFlash (canpipe_overlayPos.x > 7 && canpipe_overlayPos.y == 10 ? 1 : 0)  // https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/api/material/MaterialFinder.java#L162
#else
    const int frx_matHurt = 0;
    const int frx_matFlash = 0;
#endif

// Compat
float frx_matUnmippedFactor() { return float(frx_matUnmipped); }