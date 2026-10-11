#if defined CANPIPE_MATERIAL_SHADER

    #define frxs_baseColor Sampler0
    uniform sampler2D frxs_baseColor;

    #if defined CANPIPE_HAS_OVERLAY_POS
        #define canpipe_overlay Sampler1
        uniform sampler2D canpipe_overlay;
    #endif

    #define frxs_lightmap Sampler2
    uniform sampler2D frxs_lightmap;

    #if defined FRAGMENT_SHADER && defined SHADOW_MAP_PRESENT && !defined DEPTH_PASS
        uniform sampler2DArrayShadow frxs_shadowMap;
        uniform sampler2DArray frxs_shadowMapTexture;
    #endif

    uniform samplerBuffer canpipe_spritesExtents;

    vec2 frx_mapNormalizedUV(vec2 coord) {
        return canpipe_spriteExtents.xy + coord * (canpipe_spriteExtents.zw - canpipe_spriteExtents.xy);
    }

    vec2 frx_normalizeMappedUV(vec2 coord) {
        return (coord - canpipe_spriteExtents.xy) / (canpipe_spriteExtents.zw - canpipe_spriteExtents.xy);
    }

#endif