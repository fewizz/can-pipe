#if defined CANPIPE_MATERIAL_SHADER

    #define frxs_baseColor Sampler0
    #define canpipe_overlay Sampler1
    #define frxs_lightmap Sampler2

    uniform sampler2D frxs_baseColor;
    uniform sampler2D canpipe_overlay;
    uniform sampler2D frxs_lightmap;

    #if defined FRAGMENT_SHADER && defined SHADOW_MAP_PRESENT
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