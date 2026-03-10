// https://github.com/vram-guild/canvas/blob/9edb051bf3cba305623e6555c7d3ececb969b215/src/main/resources/assets/frex/shaders/api/vertex.glsl

#if defined CANPIPE_MATERIAL_SHADER && defined VERTEX_SHADER

    out vec4 frx_vertex;
    #if defined CANPIPE_HAS_TEXTURE_POS
        out vec2 frx_texcoord;
        flat out vec4 canpipe_spriteExtents;
    #else
        const vec2 frx_texcoord = vec2(-1);
        flat const vec4 canpipe_spriteExtents = vec4(0);
    #endif

    #if defined CANPIPE_FLAT_VERTEX_COLOR
        flat
    #endif
    out vec4 frx_vertexColor;

    flat out int canpipe_spriteIndex;
    flat out int canpipe_materialIndex;
    flat out int canpipe_materialFlags;

    #if defined CANPIPE_HAS_OVERLAY_POS
        flat out ivec2 canpipe_overlayPos;
    #endif

    #if defined DEPTH_PASS
        vec3 frx_vertexNormal = vec3(0.0);
    #else

        out vec3 frx_vertexNormal;
        out vec3 frx_vertexLight;
        out float frx_distance;
        out vec4 frx_vertexTangent;

        out vec4 frx_var0;
        out vec4 frx_var1;
        out vec4 frx_var2;
        out vec4 frx_var3;

    #endif

#endif