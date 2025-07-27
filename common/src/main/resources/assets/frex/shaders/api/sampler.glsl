#if defined CANPIPE_MATERIAL_SHADER

    uniform sampler2D frxs_baseColor;   // aka Sampler0
    uniform sampler2D canpipe_overlay;  // aka Sampler1
    uniform sampler2D frxs_lightmap;    // aka Sampler2

    #if defined FRAGMENT_SHADER && defined SHADOW_MAP_PRESENT
        uniform sampler2DArrayShadow frxs_shadowMap;
        uniform sampler2DArray frxs_shadowMapTexture;
    #endif

    uniform samplerBuffer canpipe_spritesExtents;

    vec2 frx_mapNormalizedUV(vec2 coord) {
        vec4 uv01 = texelFetch(canpipe_spritesExtents, canpipe_spriteIndex);
        return uv01.xy + coord * (uv01.zw - uv01.xy);
    }

    vec2 frx_normalizeMappedUV(vec2 coord) {
        if (canpipe_spriteIndex == -1) {
            return coord;
        }
        vec4 uv01 = texelFetch(canpipe_spritesExtents, canpipe_spriteIndex);
        return (coord - uv01.xy) / (uv01.zw - uv01.xy);
    }

#endif