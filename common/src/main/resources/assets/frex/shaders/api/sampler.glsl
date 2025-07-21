#if defined CANPIPE_MATERIAL_SHADER

    uniform sampler2D frxs_baseColor;   // aka Sampler0
    uniform sampler2D canpipe_overlay;  // aka Sampler1
    uniform sampler2D frxs_lightmap;    // aka Sampler2

    #if defined FRAGMENT_SHADER && defined SHADOW_MAP_PRESENT
        uniform sampler2DArrayShadow frxs_shadowMap;
        uniform sampler2DArray frxs_shadowMapTexture;
    #endif

    uniform sampler2D canpipe_spritesExtents;

    vec2 frx_mapNormalizedUV(vec2 coord) {
        vec4 uv0_raw = texelFetch(canpipe_spritesExtents, ivec2((canpipe_spriteIndex % 512)*2 + 0, canpipe_spriteIndex / 512), 0);
        vec4 uv1_raw = texelFetch(canpipe_spritesExtents, ivec2((canpipe_spriteIndex % 512)*2 + 1, canpipe_spriteIndex / 512), 0);
        vec2 uv0 = vec2(uv0_raw[0]*256.0 + uv0_raw[1], uv0_raw[2]*256.0 + uv0_raw[3]) / textureSize(frxs_baseColor, 0) * 255.0;
        vec2 uv1 = vec2(uv1_raw[0]*256.0 + uv1_raw[1], uv1_raw[2]*256.0 + uv1_raw[3]) / textureSize(frxs_baseColor, 0) * 255.0;
        return uv0 + coord * (uv1 - uv0);
    }

    vec2 frx_normalizeMappedUV(vec2 coord) {
        if (canpipe_spriteIndex == -1) {
            return coord;
        }
        vec4 uv0_raw = texelFetch(canpipe_spritesExtents, ivec2((canpipe_spriteIndex % 512)*2 + 0, canpipe_spriteIndex / 512), 0);
        vec4 uv1_raw = texelFetch(canpipe_spritesExtents, ivec2((canpipe_spriteIndex % 512)*2 + 1, canpipe_spriteIndex / 512), 0);
        vec2 uv0 = vec2(uv0_raw[0]*256.0 + uv0_raw[1], uv0_raw[2]*256.0 + uv0_raw[3]) / textureSize(frxs_baseColor, 0) * 255.0;
        vec2 uv1 = vec2(uv1_raw[0]*256.0 + uv1_raw[1], uv1_raw[2]*256.0 + uv1_raw[3]) / textureSize(frxs_baseColor, 0) * 255.0;
        return (coord - uv0) / (uv1 - uv0);
    }

#endif