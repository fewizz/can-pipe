package fewizz.canpipe;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class MaterialProgram extends ProgramBase {

    MaterialProgram(
        ResourceLocation location,
        VertexFormat vertexFormat,
        Shader vertexShader,
        Shader fragmentShader
    ) throws IOException, CompilationException {
        super(
            location,
            vertexFormat,
            List.of(
                new ShaderProgramConfig.Sampler("frxs_baseColor"),
                new ShaderProgramConfig.Sampler("frxs_lightmap")
            ),
            List.of(),
            vertexShader,
            fragmentShader
        );
    }

    public static MaterialProgram create(
        ShaderProgram shaderProgram,
        int glslVersion,
        boolean enablePBR,
        boolean shadowsEnabled,
        ResourceLocation pipelineLocation,
        ResourceLocation vertexLoc,
        ResourceLocation fragmentLoc,
        Map<ResourceLocation, Option> options
    ) throws FileNotFoundException, IOException, CompilationException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        String vertexSrc = IOUtils.toString(manager.getResourceOrThrow(vertexLoc).openAsReader());
        String fragmentSrc = IOUtils.toString(manager.getResourceOrThrow(fragmentLoc).openAsReader());

        String typeName = shaderProgram.configId().getPath().replace("core/", "");

        vertexSrc =
            "#define _"+typeName.toUpperCase()+"\n\n"+
            """
            in vec3 in_vertex;  // Position
            in vec4 in_color;  // Color
            in vec2 in_uv;  // UV0
            in ivec2 in_lightmap;  // UV2
            in vec3 in_normal;  // Normal

            out vec4 frx_vertex;
            out vec2 frx_texcoord;
            out vec4 frx_vertexColor;
            out vec3 frx_vertexNormal;
            // out vec4 frx_vertexTangent;
            out vec3 frx_vertexLight;
            out float frx_distance;

            """
            + vertexSrc +
            """

            void main() {
                frx_vertex = vec4(in_vertex, 1.0);
                frx_texcoord = in_uv;
                frx_vertexColor = in_color;
                frx_vertexNormal = in_normal;
                frx_vertexLight = vec3(clamp(in_lightmap / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)), 1.0);

                frx_pipelineVertex();
            }
            """;

        fragmentSrc =
            "#extension GL_ARB_conservative_depth: enable\n\n"+
            "#define _"+typeName.toUpperCase()+"\n\n"+
            (enablePBR ? "#define PBR_ENABLED\n\n" : "") +
            (shadowsEnabled ? "#define SHADOW_MAP_PRESENT\n\n" : "") +
            "const bool frx_renderTargetSolid = " + (shaderProgram == CoreShaders.RENDERTYPE_SOLID ? "true" : "false") + ";\n\n" +
            """

            layout (depth_unchanged) out float gl_FragDepth;

            // TODO?
            #define VANILLA_LIGHTING

            in vec4 frx_vertex;
            in vec2 frx_texcoord;
            in vec4 frx_vertexColor;
            in vec3 frx_vertexNormal;
            in vec3 frx_vertexLight;
            in float frx_distance;

            vec4 frx_vertexTangent = vec4(0.0);

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

            uniform sampler2D frxs_baseColor;  // aka Sampler0

            vec2 frx_mapNormalizedUV(vec2 coord) {
                // TODO
                return vec2(0.0); //_cvv_spriteBounds.xy + coord * _cvv_spriteBounds.zw;
            }

            vec2 frx_normalizeMappedUV(vec2 coord) {
                // TODO
                return vec2(0.0); // _cvv_spriteBounds.z == 0.0 ? coord : (coord - _cvv_spriteBounds.xy) / _cvv_spriteBounds.zw;
            }

            #ifdef VANILLA_LIGHTING
                uniform sampler2D frxs_lightmap;  // aka Sampler2
            #endif

            #ifdef SHADOW_MAP_PRESENT
                #ifdef FRAGMENT_SHADER
                    uniform sampler2DArrayShadow frxs_shadowMap;  // TODO define
                    uniform sampler2DArray frxs_shadowMapTexture;  // TODO define
                #endif
            #endif

            #include frex:shaders/api/material.glsl

            """
            + fragmentSrc +
            """

            #include frex:shaders/api/view.glsl

            void main() {
                frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);
                frx_fragEmissive = frx_matEmissive;
                frx_fragLight = frx_vertexLight;
                frx_fragEnableAo = frx_matDisableAo == 0;
                frx_fragEnableDiffuse = frx_matDisableDiffuse == 0;

                mat2 M = mat2(
                    dFdx(frx_texcoord.x), -dFdx(frx_texcoord.y),
                    dFdy(frx_texcoord.x), -dFdy(frx_texcoord.y)
                );

                vec2 dZ = vec2(dFdx(gl_FragCoord.z), dFdy(gl_FragCoord.z));

                vec3 p0 =
                    gl_FragCoord.xyz *
                    vec3(1.0/frx_viewWidth, 1.0/frx_viewHeight, 1.0);
                p0 = p0*2.0 - 1.0;

                vec2 advance = inverse(M)*vec2(0.0, 1.0);

                vec3 p1 =
                    (gl_FragCoord.xyz + normalize(vec3(advance, advance.x*dZ.x+advance.y*dZ.y))) *
                    vec3(1.0/frx_viewWidth, 1.0/frx_viewHeight, 1.0);
                p1 = p1*2.0 - 1.0;

                vec4 _p0 = frx_inverseProjectionMatrix * vec4(p0, 1.0);
                vec4 _p1 = frx_inverseProjectionMatrix * vec4(p1, 1.0);

                vec3 normal = mat3(frx_viewMatrix) * normalize(frx_vertexNormal.xyz);
                vec3 tangent = normalize(
                    cross(
                        _p1.xyz/_p1.w - _p0.xyz/_p0.w,
                        normal
                    )
                );
                frx_vertexTangent.xyz = mat3(frx_inverseViewMatrix) * tangent;

                #ifdef PBR_ENABLED
                    // TODO
                #endif

                frx_fragColor = frx_sampleColor * frx_vertexColor;

                #ifdef _RENDERTYPE_CUTOUT_MIPPED
                if (frx_fragColor.a < 0.5) {
                    discard;
                }
                #endif

                #ifdef _RENDERTYPE_CUTOUT
                if (frx_fragColor.a < 0.1) {
                    discard;
                }
                #endif

                // TODO handle materials here

                frx_pipelineFragment();
            }
            """;

        var vert = Shader.compile(vertexLoc, Type.VERTEX, glslVersion, options, vertexSrc);
        var frag = Shader.compile(fragmentLoc, Type.FRAGMENT, glslVersion, options, fragmentSrc);

        return new MaterialProgram(pipelineLocation.withSuffix("-"+typeName), shaderProgram.vertexFormat(), vert, frag);
    }

    @Override
    public Uniform getUniform(String name) {
        if (name.equals("ModelOffset")) { name = "frx_modelToCamera_3"; }

        return super.getUniform(name);
    }

    @Override
    public void bindSampler(String name, int id) {
        if (name.equals("Sampler0")) { name = "frxs_baseColor"; }
        if (name.equals("Sampler2")) { name = "frxs_lightmap"; }

        super.bindSampler(name, id);
    }

}
