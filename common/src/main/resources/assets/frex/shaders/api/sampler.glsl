#if defined CANPIPE_MATERIAL_SHADER

    uniform sampler2D frxs_baseColor;  // aka Sampler0
    uniform sampler2D frxs_lightmap;   // aka Sampler2

    #if defined FRAGMENT_SHADER && defined SHADOW_MAP_PRESENT
        uniform sampler2DArrayShadow frxs_shadowMap;
        uniform sampler2DArray frxs_shadowMapTexture;
    #endif

    uniform sampler2D canpipe_spritesExtents;

    vec2 frx_mapNormalizedUV(vec2 coord) {
        vec4 extents = texelFetch(
            canpipe_spritesExtents,
            ivec2(canpipe_spriteIndex % 1024, canpipe_spriteIndex / 1024),
            0
        );
        return extents.xy + coord * (extents.zw - extents.xy);
    }

    vec2 frx_normalizeMappedUV(vec2 coord) {
        if (canpipe_spriteIndex == -1) {
            return coord;
        }
        vec4 extents = texelFetch(
            canpipe_spritesExtents,
            ivec2(canpipe_spriteIndex % 1024, canpipe_spriteIndex / 1024),
            0
        );
        return (coord - extents.xy) / (extents.zw - extents.xy);
    }

#endif