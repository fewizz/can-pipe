// https://github.com/vram-guild/canvas/blob/9edb051bf3cba305623e6555c7d3ececb969b215/src/main/resources/assets/frex/shaders/api/fragment.glsl

#if defined CANPIPE_MATERIAL_SHADER && defined FRAGMENT_SHADER

    in vec4 frx_vertex;

    #if defined CANPIPE_HAS_TEXTURE_POS
        in vec2 frx_texcoord;
    #else
        const vec2 frx_texcoord = vec2(-1);
    #endif

    #if defined CANPIPE_FLAT_VERTEX_COLOR
        flat
    #endif
    in vec4 frx_vertexColor;

    flat in int canpipe_spriteIndex;
    flat in int canpipe_materialIndex;
    #if defined CANPIPE_HAS_MATERIAL_FLAGS
        flat in int canpipe_materialFlags;
    #else
        const int canpipe_materialFlags = 0;
    #endif
    #if defined CANPIPE_HAS_OVERLAY_POS
        flat in ivec2 canpipe_overlayPos;
    #endif

    vec4 frx_sampleColor = vec4(0.0);
    vec4 frx_fragColor = vec4(0.0);
    vec3 frx_fragLight = vec3(0.0);
    bool frx_fragEnableAo = false;
    bool frx_fragEnableDiffuse = false;
    float frx_fragEmissive = 0.0;

    #if !defined DEPTH_PASS
        in vec3 frx_vertexLight;
        in vec3 frx_vertexNormal;
        in float frx_distance;
        in vec4 frx_vertexTangent;

        in vec4 frx_var0;
        in vec4 frx_var1;
        in vec4 frx_var2;
        in vec4 frx_var3;

        #if defined PBR_ENABLED
            float frx_fragReflectance = 0.04;
            vec3 frx_fragNormal = vec3(0.0, 0.0, 1.0);
            float frx_fragHeight = 0.0;
            float frx_fragRoughness = 1.0;
            float frx_fragAo = 1.0;
        #endif

    #endif

#endif