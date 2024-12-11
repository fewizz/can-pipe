package fewizz.canpipe.pipeline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.shaders.CompiledShader.Type;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.TextureAtlasAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager.CompilationException;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class MaterialProgram extends ProgramBase {

    MaterialProgram(
        ResourceLocation location,
        VertexFormat vertexFormat,
        Shader vertexShader,
        Shader fragmentShader,
        List<String> samplers,
        List<? extends AbstractTexture> textures
    ) throws IOException, CompilationException {
        super(
            location,
            vertexFormat,
            List.of("frxs_baseColor", "frxs_lightmap", "canpipe_spritesExtents"),
            samplers,
            List.of(),
            vertexShader,
            fragmentShader
        );
        if (samplers.size() > textures.size()) {
            CanPipe.LOGGER.warn("Material program "+location+" has more samplers than textures");
        }
        if (samplers.size() < textures.size()) {
            CanPipe.LOGGER.warn("Material program "+location+" has less samplers than textures");
        }
        for (int i = 0; i < Math.min(samplers.size(), textures.size()); ++i) {
            String sampler = samplers.get(i);
            AbstractTexture texture = textures.get(i);
            if (texture == null) {
                if (!this.samplerExists(sampler)) {
                    continue;
                }
                throw new NullPointerException("Couldn't find texture for sampler \"" + sampler + "\"");
            }
            bindSampler(sampler, texture);
        }
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
        List<String> samplers,
        List<? extends AbstractTexture> textures,
        Map<ResourceLocation, String> shaderSourceCache
    ) throws FileNotFoundException, IOException, CompilationException {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        String vertexSrc = IOUtils.toString(manager.getResourceOrThrow(vertexLoc).openAsReader());
        String fragmentSrc = IOUtils.toString(manager.getResourceOrThrow(fragmentLoc).openAsReader());

        String typeName = shaderProgram.configId().getPath().replace("core/", "");

        String shadowMapDefinitions = null;

        if (shadowFramebuffer != null && shadowFramebuffer.depthAttachement != null) {
            var depthArray = shadowFramebuffer.depthAttachement.texture();

            samplers = Streams.concat(
                samplers.stream(),
                List.of("frxs_shadowMap", "frxs_shadowMapTexture").stream()
            ).toList();
            textures = Streams.concat(
                textures.stream(),
                List.of(depthArray, depthArray).stream()
            ).toList();

            shadowMapDefinitions =
                "#define SHADOW_MAP_PRESENT\n"+
                "#define SHADOW_MAP_SIZE "+depthArray.extent.x;
        }

        var format = shaderProgram.vertexFormat();
        if (format == DefaultVertexFormat.BLOCK) {
            format = CanPipe.VertexFormats.BLOCK;
        }
        if (format == DefaultVertexFormat.NEW_ENTITY) {
            format = CanPipe.VertexFormats.NEW_ENTITY;
        }
        if (format == DefaultVertexFormat.PARTICLE) {
            format = CanPipe.VertexFormats.PARTICLE;
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
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.POSITION)+") in vec3 in_vertex;  // Position\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.COLOR)+") in vec4 in_color;  // Color\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV0)+") in vec2 in_uv;  // UV0\n"+
            (format.contains(VertexFormatElement.UV1) ? "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV1)+") in ivec2 in_uv1" : "const ivec2 in_v1 = ivec2(0)") + ";\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV2)+") in ivec2 in_lightmap;  // UV2\n"+
            (format.contains(VertexFormatElement.NORMAL) ? "layout(location = "+format.getElements().indexOf(VertexFormatElement.NORMAL)+") in vec3 in_normal" : "const vec3 in_normal = vec3(0.0, 1.0, 0.0)") + ";  // Normal\n"+
            (format.contains(CanPipe.VertexFormatElements.AO) ? "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.AO)+") in float in_ao" : "const float in_ao = 1.0") + ";\n"+
            (format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX) ? "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.SPRITE_INDEX)+") in int in_spriteIndex" : "const int in_spriteIndex = -1") + ";\n"+
            (format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX) ? "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_INDEX)+") in int in_materialIndex" : "const int in_materialIndex = -1") + ";\n"+
            (format.contains(CanPipe.VertexFormatElements.TANGENT) ? "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.TANGENT)+") in vec4 in_vertexTangent" : "const vec4 in_vertexTangent = vec4(1.0)") + ";\n"+
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
            usedMaterialIDs.intStream().mapToObj(id ->
            "        case "+id+": _material_"+id+"(); break;\n"
            ).collect(Collectors.joining()) +
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
                uniform sampler2DArrayShadow frxs_shadowMap;  // TODO define
                uniform sampler2DArray frxs_shadowMapTexture;  // TODO define
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

                #if defined _RENDERTYPE_CUTOUT_MIPPED
                if (frx_fragColor.a < 0.5) {
                    discard;
                }
                #endif

                #if defined _RENDERTYPE_CUTOUT || defined _RENDERTYPE_ENTITY_CUTOUT || defined _RENDERTYPE_ENTITY_CUTOUT_NO_CULL || defined _RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET || defined _PARTICLE || defined _RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL
                if (frx_fragColor.a < 0.1) {
                    discard;
                }
                #endif

                """+
            "    switch (canpipe_materialIndex) {\n" +
            usedMaterialIDs.intStream().mapToObj(id ->
            "        case "+id+": _material_"+id+"(); break;\n"
            ).collect(Collectors.joining()) +
            "    default: ;\n"+
            "    }\n\n"+
            """

                frx_pipelineFragment();
            }
            """;

        var vertexShader = Shader.compile(vertexLoc, Type.VERTEX, glslVersion, options, vertexSrc, shaderSourceCache);
        var fragmentShader = Shader.compile(fragmentLoc, Type.FRAGMENT, glslVersion, options, fragmentSrc, shaderSourceCache);

        return new MaterialProgram(
            pipelineLocation.withSuffix("-"+typeName),
            format,
            vertexShader,
            fragmentShader,
            samplers,
            textures
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void bindSampler(String name, int id) {
        if (name.equals("Sampler0")) {
            name = "frxs_baseColor";

            // cursed, as always :)
            var mc = Minecraft.getInstance();
            for (var atlasLoc : ModelManager.VANILLA_ATLASES.keySet()) {
                var atlas = mc.getModelManager().getAtlas(atlasLoc);
                if (atlas.getId() == id) {
                    super.bindSampler("canpipe_spritesExtents", ((TextureAtlasAccessor) atlas).getSpriteData());
                }
            }
        }
        if (name.equals("Sampler2")) { name = "frxs_lightmap"; }

        super.bindSampler(name, id);
    }

}
