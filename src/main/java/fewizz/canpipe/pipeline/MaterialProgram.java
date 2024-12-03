package fewizz.canpipe.pipeline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipeVertexFormatElements;
import fewizz.canpipe.CanPipeVertexFormats;
import fewizz.canpipe.Material;
import fewizz.canpipe.Materials;
import fewizz.canpipe.Pipelines;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderProgramConfig;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class MaterialProgram extends ProgramBase {

    final List<AbstractTexture> samplerImages;

    MaterialProgram(
        ResourceLocation location,
        VertexFormat vertexFormat,
        Shader vertexShader,
        Shader fragmentShader,
        List<ShaderProgramConfig.Sampler> samplers,
        List<AbstractTexture> samplerImages,
        boolean shadowsEnabled
    ) throws IOException, CompilationException {
        super(
            location,
            vertexFormat,
            Stream.concat(
                (
                    shadowsEnabled ?
                    List.of(
                        new ShaderProgramConfig.Sampler("frxs_baseColor"),
                        new ShaderProgramConfig.Sampler("canpipe_spritesExtents"),
                        new ShaderProgramConfig.Sampler("frxs_lightmap"),
                        new ShaderProgramConfig.Sampler("frxs_shadowMap"),
                        new ShaderProgramConfig.Sampler("frxs_shadowMapTexture")
                    ) :
                    List.of(
                        new ShaderProgramConfig.Sampler("frxs_baseColor"),
                        new ShaderProgramConfig.Sampler("canpipe_spritesExtents"),
                        new ShaderProgramConfig.Sampler("frxs_lightmap")
                    )
                ).stream(),
                samplers.stream()
            ).toList(),
            List.of(),
            vertexShader,
            fragmentShader,
            samplers.stream().map(s -> s.name()).toList()
        );
        this.samplerImages = samplerImages;
    }

    public static MaterialProgram create(
        ShaderProgram shaderProgram,
        int glslVersion,
        boolean enablePBR,
        @Nullable Framebuffer shadowFramebuffer,
        ResourceLocation pipelineLocation,
        ResourceLocation vertexLoc,
        ResourceLocation fragmentLoc,
        Map<ResourceLocation, Option> options,
        List<ShaderProgramConfig.Sampler> samplers,
        List<AbstractTexture> samplerImages,
        Map<ResourceLocation, String> shaderSourceCache
    ) throws FileNotFoundException, IOException, CompilationException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        String vertexSrc = IOUtils.toString(manager.getResourceOrThrow(vertexLoc).openAsReader());
        String fragmentSrc = IOUtils.toString(manager.getResourceOrThrow(fragmentLoc).openAsReader());

        String typeName = shaderProgram.configId().getPath().replace("core/", "");

        String shadowMapDefinitions = null;

        if (shadowFramebuffer != null && shadowFramebuffer.depthAttachement != null) {
            var depthArray = shadowFramebuffer.depthAttachement.texture();
            shadowMapDefinitions =
                "#define SHADOW_MAP_PRESENT\n"+
                "#define SHADOW_MAP_SIZE "+depthArray.extent.x;
        }

        var format = shaderProgram.vertexFormat();
        if (format == DefaultVertexFormat.BLOCK) {
            format = CanPipeVertexFormats.BLOCK;
        }
        if (format == DefaultVertexFormat.NEW_ENTITY) {
            format = CanPipeVertexFormats.NEW_ENTITY;
        }

        String materialsVertexSrc = "";
        IntList usedMaterialIDs = new IntArrayList();
        for (Material m : Materials.MATERIALS.values()) {
            if (m.vertexShaderSource == null) {
                continue;
            }
            int id = Materials.id(m);
            String src = m.vertexShaderSource.replace("frx_materialVertex", "_material_"+id);
            materialsVertexSrc += src + "\n\n";
            usedMaterialIDs.add(id);
        }

        int location = 0;

        String uvMapping =
            """
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
                return (coord - extents.xy) / (extents.zw - extents.xy); // (coord / vec2(textureSize(frxs_baseColor, 0))) - extents.xy;
            }
            """;

        vertexSrc =
            "#define _"+typeName.toUpperCase()+"\n\n"+
            (shadowMapDefinitions != null ? shadowMapDefinitions + "\n\n" : "")+
            "layout(location = "+(location++)+") in vec3 in_vertex;  // Position\n"+
            "layout(location = "+(location++)+") in vec4 in_color;  // Color\n"+
            "layout(location = "+(location++)+") in vec2 in_uv;  // UV0\n"+
            (format.contains(VertexFormatElement.UV1) ? "layout(location = "+(location++)+") in ivec2 in_uv1" : "const ivec2 in_v1 = ivec2(0)") + ";\n"+
            "layout(location = "+(location++)+") in ivec2 in_lightmap;  // UV2\n"+
            "layout(location = "+(location++)+") in vec3 in_normal;  // Normal\n"+
            (format.contains(CanPipeVertexFormatElements.AO) ? "layout(location = "+(location++)+") in float in_ao" : "const float in_ao = 1.0") + ";\n"+
            (format.contains(CanPipeVertexFormatElements.SPRITE_INDEX) ? "layout(location = "+(location++)+") in int in_spriteIndex" : "const int in_spriteIndex = -1") + ";\n"+
            (format.contains(CanPipeVertexFormatElements.MATERIAL_INDEX) ? "layout(location = "+(location++)+") in int in_materialIndex" : "const int in_materialIndex = -1") + ";\n"+
            (format.contains(CanPipeVertexFormatElements.TANGENT) ? "layout(location = "+(location++)+") in vec4 in_vertexTangent" : "const vec4 in_vertexTangent = vec4(1.0)") + ";\n"+
            """

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

            uniform vec3 canpipe_light0Direction;  // aka Light0_Direction
            uniform vec3 canpipe_light1Direction;  // aka Light1_Direction

            """ +
            uvMapping +
            materialsVertexSrc +
            vertexSrc +
            """

            #include frex:shaders/api/view.glsl

            void main() {
                frx_vertex = vec4(in_vertex, 1.0);
                frx_texcoord = in_uv;
                frx_vertexColor = in_color;
                frx_vertexNormal = in_normal;
                frx_vertexLight = vec3(
                    clamp(
                        in_lightmap / 256.0,
                        vec2(0.5 / 16.0),
                        vec2(15.5 / 16.0)
                    ),
                    in_ao
                );
                frx_vertexTangent = in_vertexTangent;
                canpipe_spriteIndex = in_spriteIndex;
                canpipe_materialIndex = in_materialIndex;

                if (frx_isGui && !frx_isHand) {
                    frx_vertexNormal.y *= -1.0;  // compat
                }

            """+
            "    switch (in_materialIndex) {\n" +
            usedMaterialIDs.intStream().mapToObj(id -> "    case "+id+": _material_"+id+"(); break;\n").collect(Collectors.joining()) +
            "    default: ;\n"+
            "    }\n\n"+
            """
                frx_pipelineVertex();
            }
            """;

        String materialsFragmentSrc = "";
        usedMaterialIDs.clear();
        for (Material m : Materials.MATERIALS.values()) {
            if (m.fragmentShaderSource == null) {
                continue;
            }
            int id = Materials.id(m);
            String src = m.fragmentShaderSource.replace("frx_materialFragment", "_material_"+id);
            materialsFragmentSrc += src + "\n\n";
            usedMaterialIDs.add(id);
        }

        fragmentSrc =
            "#extension GL_ARB_conservative_depth: enable\n\n"+
            "#define _"+typeName.toUpperCase()+"\n\n"+
            (enablePBR ? "#define PBR_ENABLED\n\n" : "") +
            (shadowMapDefinitions != null ? shadowMapDefinitions + "\n\n" : "") +
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
            in vec4 frx_vertexTangent;
            flat in int canpipe_spriteIndex;
            flat in int canpipe_materialIndex;

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

            uniform sampler2D frxs_baseColor;  // aka Sampler0

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

            """ +
            uvMapping +
            materialsFragmentSrc +
            fragmentSrc +
            """

            #include frex:shaders/api/view.glsl

            void main() {
                frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);
                frx_fragEmissive = frx_matEmissive;
                frx_fragLight = frx_vertexLight;
                frx_fragEnableAo = frx_matDisableAo == 0;
                frx_fragEnableDiffuse = frx_matDisableDiffuse == 0;

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

                """+
            "    switch (canpipe_materialIndex) {\n" +
            usedMaterialIDs.intStream().mapToObj(id -> "    case "+id+": _material_"+id+"(); break;\n").collect(Collectors.joining()) +
            "    default: ;\n"+
            "    }\n\n"+
            """

                frx_pipelineFragment();
            }
            """;

        var vert = Shader.compile(vertexLoc, Type.VERTEX, glslVersion, options, vertexSrc, shaderSourceCache);
        var frag = Shader.compile(fragmentLoc, Type.FRAGMENT, glslVersion, options, fragmentSrc, shaderSourceCache);

        return new MaterialProgram(
            pipelineLocation.withSuffix("-"+typeName),
            format,
            vert,
            frag,
            samplers,
            samplerImages,
            shadowFramebuffer != null
        );
    }

    @Override
    public void apply() {
        var p = Pipelines.getCurrent();
        if (p != null && p.skyShadows != null) {
            var fb = p.framebuffers.get(p.skyShadows.framebufferName());
            var tex = fb.depthAttachement.texture();
            bindSampler("frxs_shadowMap", tex);
            bindSampler("frxs_shadowMapTexture", tex);
        }

        bindExpectedSamplers(samplerImages);

        super.apply();
    }

    @Override
    public void bindSampler(String name, int id) {
        if (name.equals("Sampler0")) {
            name = "frxs_baseColor";

            // cursed, as always :)
            var mc = Minecraft.getInstance();
            for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                var atlas = mc.getModelManager().getAtlas(atlasLoc);
                if (atlas.getId() == id) {
                    super.bindSampler("canpipe_spritesExtents", ((TextureAtlasExtended) atlas).getSpriteData().getId());
                }
            }
        }

        if (name.equals("Sampler2")) { name = "frxs_lightmap"; }

        super.bindSampler(name, id);
    }

    @Override
    public void setDefaultUniforms(Mode mode, Matrix4f matrix4f, Matrix4f matrix4f2, Window window) {
        super.setDefaultUniforms(mode, matrix4f, matrix4f2, window);

    }

}
