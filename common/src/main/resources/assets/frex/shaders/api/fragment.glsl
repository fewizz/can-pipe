#if defined CANPIPE_MATERIAL_SHADER && defined FRAGMENT_SHADER

    in vec4 frx_vertex;
    in vec2 frx_texcoord;
    in vec4 frx_vertexColor;
    in vec3 frx_vertexNormal;
    in vec4 frx_vertexTangent;

    in vec3 frx_vertexLight;
    in float frx_distance;
    flat in int canpipe_spriteIndex;
    flat in int canpipe_materialIndex;
    flat in int canpipe_materialFlags;

    in vec4 frx_var0;
    in vec4 frx_var1;
    in vec4 frx_var2;
    in vec4 frx_var3;

    vec4 frx_sampleColor = vec4(0.0);
    vec4 frx_fragColor = vec4(0.0);
    vec3 frx_fragLight = vec3(0.0);
    bool frx_fragEnableAo = false;
    bool frx_fragEnableDiffuse = false;
    float frx_fragEmissive = 0.0;

    #ifdef PBR_ENABLED
        float frx_fragReflectance = 0.04;
        vec3 frx_fragNormal = vec3(0.0, 0.0, 1.0);
        float frx_fragHeight = 0.0;
        float frx_fragRoughness = 1.0;
        float frx_fragAo = 1.0;
    #endif // PBR

#endif