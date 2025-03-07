#if defined CANPIPE_MATERIAL_SHADER && defined VERTEX_SHADER

    out vec4 frx_vertex;
    out vec2 frx_texcoord;
    out vec4 frx_vertexColor;
    out vec3 frx_vertexNormal;
    out vec3 frx_vertexLight;
    out float frx_distance;
    out vec4 frx_vertexTangent;

    out vec4 frx_var0;
    out vec4 frx_var1;
    out vec4 frx_var2;
    out vec4 frx_var3;

    flat out int canpipe_spriteIndex;
    flat out int canpipe_materialIndex;
    flat out int canpipe_materialFlags;

# endif