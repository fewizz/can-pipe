package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.function.TriConsumer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.b3d.RenderPipelineBuilderExtended;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMaps;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;

public class MaterialPrograms {

    private MaterialPrograms() {}

    static MaterialProgramLoader load(
        RenderPipeline originalRenderPipeline,
        int glslVersion,
        boolean enablePBR,
        boolean shadow,
        Optional<Integer> shadowMapSize,
        Identifier vertexShaderLocation,
        Identifier fragmentShaderLocation,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        List<String> samplers,
        Function<Identifier, Optional<String>> getShaderSource,
        float shadowsOffsetSlopeFactor,
        float shadowsOffsetBiasUnits,
        boolean awareOfDepthRangeChanges
    ) {
        VertexFormat vertexFormat;
        if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.BLOCK) {
            vertexFormat = CanPipe.VertexFormats.BLOCK;
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.ENTITY) {
            vertexFormat = !shadow ? CanPipe.VertexFormats.ENTITY : CanPipe.VertexFormats.ENTITY_SHADOW;
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.PARTICLE) {
            vertexFormat = !shadow ? CanPipe.VertexFormats.PARTICLE : CanPipe.VertexFormats.PARTICLE_SHADOW;
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_COLOR_LIGHTMAP) {
            vertexFormat = CanPipe.VertexFormats.POSITION_COLOR_LIGHTMAP;
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
            vertexFormat = CanPipe.VertexFormats.POSITION_COLOR_TEX_LIGHTMAP;
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR) {
            vertexFormat = CanPipe.VertexFormats.POSITION_TEX_LIGHTMAP_COLOR;
        }
        else {
            throw new RuntimeException("Unexpected vertex format to replace: "+originalRenderPipeline.getVertexFormatBinding(0).toString());
        }

        Identifier vertexShaderIDWithPostfix = vertexShaderLocation.withSuffix("/"+originalRenderPipeline.getLocation().getPath()+".vsh");
        Identifier fragmentShaderIDWithPostfix = fragmentShaderLocation.withSuffix("/"+originalRenderPipeline.getLocation().getPath()+".fsh");

        Identifier location = Identifier.fromNamespaceAndPath(
            CanPipe.MOD_ID, (!shadow ? "material" : "material_shadow")+"-"+originalRenderPipeline.getLocation().getPath()
        );

        var renderPipelineBuilder = RenderPipeline.builder();
        {
            renderPipelineBuilder
                .withLocation(location)
                .withVertexShader(vertexShaderIDWithPostfix)
                .withFragmentShader(fragmentShaderIDWithPostfix)
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(!shadow ? originalRenderPipeline.isCull() : false)
                .withVertexBinding(0, vertexFormat)
                .withPrimitiveTopology(originalRenderPipeline.getPrimitiveTopology());

            ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_allowNoColorTargets();

            var dsState = originalRenderPipeline.getDepthStencilState();
            if (dsState != null) {
                CompareOp compareOp = dsState.depthTest();

                if (!awareOfDepthRangeChanges) {
                    if      (compareOp == CompareOp.LESS_THAN) { compareOp = CompareOp.GREATER_THAN; }
                    else if (compareOp == CompareOp.GREATER_THAN) { compareOp = CompareOp.LESS_THAN; }
                    else if (compareOp == CompareOp.LESS_THAN_OR_EQUAL) { compareOp = CompareOp.GREATER_THAN_OR_EQUAL;}
                    else if (compareOp == CompareOp.GREATER_THAN_OR_EQUAL) { compareOp = CompareOp.LESS_THAN_OR_EQUAL;}
                }

                renderPipelineBuilder.withDepthStencilState(new DepthStencilState(
                    compareOp,
                    dsState.writeDepth(),
                    !shadow ? dsState.depthBiasScaleFactor() : shadowsOffsetSlopeFactor,
                    !shadow ? dsState.depthBiasConstant() : shadowsOffsetBiasUnits
                ));
            }
        }

        boolean terrain = originalRenderPipeline.getBindGroupLayouts().stream().anyMatch(bgl -> bgl == BindGroupLayouts.CHUNK_SECTION);

        if (terrain) {
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION);
        }
        else {
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
        }
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.PROJECTION);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.FOG);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.GLOBALS);

        /*
        ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_withOptionalSampler("Sampler0");
        ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_withOptionalSampler("Sampler1");
        ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_withOptionalSampler("Sampler2");*/

        // TODO
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2);

        var bindGroupLayoutBuilder = BindGroupLayout.builder()
            .withUniform("canpipe_ub_render_target", UniformType.UNIFORM_BUFFER)
            .withUniform("canpipe_ub_origin_type", UniformType.UNIFORM_BUFFER)
            .withUniform("canpipe_ub_is_rendering_hand", UniformType.UNIFORM_BUFFER)

            .withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_shadow", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);
        if (shadow) {
            bindGroupLayoutBuilder.withUniform("frxu_ub_cascade", UniformType.UNIFORM_BUFFER);
        }

        bindGroupLayoutBuilder.withUniform("canpipe_spritesExtents", UniformType.TEXEL_BUFFER, GpuFormat.RGBA16_UNORM);

        for (String sampler : samplers) {
            bindGroupLayoutBuilder.withSampler(sampler);
        }
        renderPipelineBuilder.withBindGroupLayout(bindGroupLayoutBuilder.build());

        Collection<Material> materials;

        if (originalRenderPipeline == RenderPipelines.SOLID_TERRAIN || originalRenderPipeline == RenderPipelines.SOLID_BLOCK) {
            materials = MaterialMaps.getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer.SOLID);
        }
        else if (originalRenderPipeline == RenderPipelines.CUTOUT_TERRAIN || originalRenderPipeline == RenderPipelines.CUTOUT_BLOCK) {
            materials = MaterialMaps.getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer.CUTOUT);
        }
        else if (originalRenderPipeline == RenderPipelines.TRANSLUCENT_TERRAIN || originalRenderPipeline == RenderPipelines.TRANSLUCENT_BLOCK) {
            materials = MaterialMaps.getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer.TRANSLUCENT);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.ENTITY) {
            materials = MaterialMaps.getAllUsedMaterials();
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.PARTICLE) {
            materials = MaterialMaps.getMaterialsUsedByParticles();
        }
        else {
            materials = Collections.emptyList();
        }

        Function<String, String> postprocess = (String src) -> {
            // These three ideally shouldn't be in a material shader, but it's still possible
            src = src.replaceAll("uniform\\s+ivec2\\s+frxu_size;", "const ivec2 frxu_size = ivec2(-1);");
            src = src.replaceAll("uniform\\s+int\\s+frxu_lod;", "const int frxu_lod = -1;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_layer;", "const int frxu_layer = -1;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_cascade;", "// uniform int frxu_cascade;");

            if (shadow) {
                src = "\n"+
                    "layout(std140) uniform frxu_ub_cascade {\n"+
                    "    int frxu_cascade;\n"+
                    "};\n\n"+
                    src;
            }

            src =
                "layout(std140) uniform canpipe_ub_render_target {\n"+
                "    int canpipe_renderTarget;\n"+
                "};\n\n"+
                "layout(std140) uniform canpipe_ub_origin_type {\n"+
                "    int canpipe_originType;\n"+
                "};\n\n"+
                "layout(std140) uniform canpipe_ub_is_rendering_hand {\n"+
                "    int canpipe_isRenderingHand;\n"+
                "};\n\n"+
                src;

            return src;
        };

        TriConsumer<String, Identifier, String> onCompilationError = (String log, Identifier shaderLocation, String src) -> {
            Path compilationErrorsPath = CanPipe.getCompilationErrorsDirPath();
            try {
                Files.createDirectories(compilationErrorsPath);
                Files.writeString(
                    compilationErrorsPath.resolve(shaderLocation.toString().replace("/", "--").replace(":", "--")),
                    src+"\n"+log
                );
            } catch (IOException e) {
                CanPipe.LOGGER.warn("Couldn't save \""+shaderLocation.toString()+"\" compilation error", e);
            }
            throw new RuntimeException("Couldn't compile \""+shaderLocation.toString()+"\": "+log);
        };

        String vertexSrc = getVertexSrc(vertexShaderLocation, getShaderSource, vertexFormat, originalRenderPipeline, materials, shadow, terrain, enablePBR);
        String fragmentSrc = getFragmentSrc(fragmentShaderLocation, getShaderSource, vertexFormat, originalRenderPipeline, materials, shadow, terrain, enablePBR);

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineModule(
            vertexShaderIDWithPostfix,
            Shaders.process(
                vertexShaderLocation, vertexSrc, ShaderType.VERTEX, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ),
            ShaderType.VERTEX,
            onCompilationError
        );

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineModule(
            fragmentShaderIDWithPostfix,
            Shaders.process(
                fragmentShaderLocation, fragmentSrc, ShaderType.FRAGMENT, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ),
            ShaderType.FRAGMENT,
            onCompilationError
        );

        return new MaterialProgramLoader(location, renderPipelineBuilder, originalRenderPipeline);
    }

    private static String getVertexSrc(
        Identifier vertexShaderLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        VertexFormat vertexFormat,
        RenderPipeline originalRenderPipeline,
        Collection<Material> materials,
        boolean shadow,
        boolean terrain,
        boolean enablePBR
    ) {
        String vertexSrcOriginal = getShaderSource.apply(vertexShaderLocation).get();

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = vertexFormat.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        boolean hasOverlayPos = vertexFormat.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);
        boolean hasLightmapPos = vertexFormat.contains(DefaultVertexFormat.UV2_SEMANTIC_NAME);
        boolean hasMaterialFlags = vertexFormat.contains(CanPipe.VertexFormats.MATERIAL_FLAGS_ATTRIBUTE_NAME);
        boolean hasMaterialIndex = vertexFormat.contains(CanPipe.VertexFormats.MATERIAL_INDEX_ATTRIBUTE_NAME);

        StringBuilder materialsFunctionsSrc = new StringBuilder();
        StringBuilder materialsSwitchSrc = new StringBuilder();

        if (hasMaterialIndex) {
            materialsSwitchSrc.append("    switch (canpipe_materialIndex) {\n");

            for (Material m : materials) {
                String src = shadow ? m.depthVertexShaderSource() : m.vertexShaderSource();
                if (src == null) {
                    continue;
                }
                src = src.replace("frx_materialFragment", "_material_"+m.index());

                String materialFunctionName = "_material_"+m.index();

                materialsSwitchSrc.append("        case "+m.index()+": "+materialFunctionName+"(); break;\n");
                materialsFunctionsSrc.append(src.replace("frx_materialVertex", materialFunctionName) + "\n\n");
            }

            materialsSwitchSrc.append("        default: break;\n");
            materialsSwitchSrc.append("    }\n");
        }

        var vertexSrcBuilder = new StringBuilder();

        vertexSrcBuilder.append("#define in_vertex Position\n");
        vertexSrcBuilder.append("#define in_color Color\n");
        vertexSrcBuilder.append("#define in_uv UV0\n");
        vertexSrcBuilder.append("#define in_overlayPos UV1\n");
        vertexSrcBuilder.append("#define in_lightmapPos UV2\n");
        vertexSrcBuilder.append("#define in_normal Normal\n");

        vertexSrcBuilder.append("#define CANPIPE_MATERIAL_SHADER\n");
        if (terrain) {
            vertexSrcBuilder.append("#define CANPIPE_TERRAIN\n");
        }
        if (shadow) {
            vertexSrcBuilder.append("#define DEPTH_PASS\n");
        }
        if (enablePBR) {
            vertexSrcBuilder.append("#define PBR_ENABLED\n");
        }
        if (flatVertexColor) {
            vertexSrcBuilder.append("#define CANPIPE_FLAT_VERTEX_COLOR\n");
        }
        vertexSrcBuilder.append("\n");
        vertexSrcBuilder.append("\n");
        vertexSrcBuilder.append("in vec3 in_vertex;  // Position\n");
        vertexSrcBuilder.append("in vec4 in_color;  // Color\n");
        if (hasTexturePos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_TEXTURE_POS\n");
            vertexSrcBuilder.append("in vec2 in_uv;  // UV0\n");
        }
        if (hasOverlayPos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_OVERLAY_POS\n");
            vertexSrcBuilder.append("in ivec2 in_overlayPos;  // UV1\n");
        }
        if (hasLightmapPos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_LIGHTMAP_POS\n");
            vertexSrcBuilder.append("in ivec2 in_lightmapPos;  // UV2\n");
        }
        if (hasMaterialFlags) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_FLAGS\n");
            vertexSrcBuilder.append("in int in_materialFlags;\n");
        }
        if (hasMaterialIndex) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_INDEX\n");
            vertexSrcBuilder.append("in int in_materialIndex;\n");
        }
        vertexSrcBuilder.append(
            vertexFormat.contains(DefaultVertexFormat.NORMAL_SEMANTIC_NAME) ?
            "in vec3 in_normal; // Normal\n" :
            "const vec3 in_normal = vec3(0.0, 1.0, 0.0);  // Normal\n"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormats.AO_ATTRIBUTE_NAME) ?
            "in float in_ao;\n" :
            "const float in_ao = 1.0;\n"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormats.SPRITE_INDEX_ATTRIBUTE_NAME) ?
            "in int in_spriteIndex;\n" :
            "const int in_spriteIndex = -1;\n"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormats.TANGENT_ATTRIBUTE_NAME) ?
            "in vec4 in_tangent;\n" :
            "const vec4 in_tangent = vec4(1.0);\n"
        );
        vertexSrcBuilder.append(
        """

        #include frex:shaders/api/vertex.glsl
        #include frex:shaders/api/sampler.glsl
        #include frex:shaders/api/material.glsl
        #include frex:shaders/api/view.glsl
        #include frex:shaders/api/header.glsl
        #include frex:shaders/api/world.glsl

        """
        );
        vertexSrcBuilder.append(materialsFunctionsSrc);
        vertexSrcBuilder.append(vertexSrcOriginal);
        vertexSrcBuilder.append(
        """

        void main() {
            frx_vertex = vec4(in_vertex, 1.0);

            #if defined CANPIPE_TERRAIN
                // Losing precision (could be part of `frx_modelToCamera`), but pipelines rely on this behaviour
                frx_vertex.xyz += ChunkPosition;
            #endif

            canpipe_spriteIndex = in_spriteIndex;
            #if defined CANPIPE_HAS_MATERIAL_INDEX
                canpipe_materialIndex = in_materialIndex;
            #endif
            #if defined CANPIPE_HAS_MATERIAL_FLAGS
                canpipe_materialFlags = in_materialFlags;
            #endif

            #if defined CANPIPE_HAS_TEXTURE_POS
                if (
                    #if defined CANPIPE_TERRAIN
                        true
                    #else
                        canpipe_spriteIndex != -1
                    #endif
                ) {
                    canpipe_spriteExtents = texelFetch(canpipe_spritesExtents, canpipe_spriteIndex);
                    frx_texcoord = frx_normalizeMappedUV(in_uv);
                }
                else {
                    canpipe_spriteExtents = vec4(0.0, 0.0, 1.0, 1.0);
                    frx_texcoord = in_uv;
                }
            #endif

            frx_vertexColor = canpipe_disableColorIndex == 0 ? in_color : vec4(1.0);

            #if !defined DEPTH_PASS
                frx_vertexNormal = in_normal;
                frx_vertexTangent = in_tangent;
                #if defined CANPIPE_HAS_LIGHTMAP_POS
                    frx_vertexLight = vec3(clamp(in_lightmapPos / 256.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)), in_ao);
                #else
                    frx_vertexLight = vec3(1.0);
                #endif
            #endif

            #if defined CANPIPE_HAS_OVERLAY_POS
                canpipe_overlayPos = in_overlayPos;
            #endif

            if (frx_isGui && !frx_isHand) frx_vertexNormal.y *= -1.0;  // compat
        """);
        vertexSrcBuilder.append(materialsSwitchSrc);
        vertexSrcBuilder.append(
        """

            #if defined CANPIPE_HAS_TEXTURE_POS
                frx_texcoord = in_uv;
            #endif

            frx_pipelineVertex();
        }
        """
        );

        return vertexSrcBuilder.toString();
    }

    private static String getFragmentSrc(
        Identifier fragmentShaderLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        VertexFormat vertexFormat,
        RenderPipeline originalRenderPipeline,
        Collection<Material> materials,
        boolean shadow,
        boolean terrain,
        boolean enablePBR
    ) {
        String fragmentSrcOriginal = getShaderSource.apply(fragmentShaderLocation).get();

        StringBuilder materialsFunctionsSrc = new StringBuilder();
        StringBuilder materialsSwitchSrc = new StringBuilder();

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = vertexFormat.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        boolean hasOverlayPos = vertexFormat.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);
        boolean hasMaterialFlags = vertexFormat.contains(CanPipe.VertexFormats.MATERIAL_FLAGS_ATTRIBUTE_NAME);
        boolean hasMaterialIndex = vertexFormat.contains(CanPipe.VertexFormats.MATERIAL_INDEX_ATTRIBUTE_NAME);

        if (hasMaterialIndex) {
            materialsSwitchSrc.append("    switch (canpipe_materialIndex) {\n");

            for (Material m : materials) {
                String src = shadow ? m.depthFragmentShaderSource() : m.fragmentShaderSource();
                if (src == null) {
                    continue;
                }
                src = src.replace("frx_materialFragment", "_material_"+m.index());

                String materialFunctionName = "_material_"+m.index();

                materialsSwitchSrc.append("        case "+m.index()+": "+materialFunctionName+"(); break;\n");
                materialsFunctionsSrc.append(src.replace("frx_materialFragment", materialFunctionName) + "\n\n");
            }

            materialsSwitchSrc.append("        default: break;\n");
            materialsSwitchSrc.append("    }\n");
        }

        Float alphaCutout = null;

        if (
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_BLOCK ||
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_TERRAIN
        ) {
            alphaCutout = 0.01F;
        }
        else if (
            originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.PARTICLE ||

            originalRenderPipeline == RenderPipelines.GLINT ||
            originalRenderPipeline == RenderPipelines.LINES ||
            originalRenderPipeline == RenderPipelines.SECONDARY_BLOCK_OUTLINE ||
            originalRenderPipeline == RenderPipelines.LINES_TRANSLUCENT ||

            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_CULL ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_Z_OFFSET ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_DISSOLVE ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_CULL ||

            originalRenderPipeline == RenderPipelines.ITEM_CUTOUT ||
            originalRenderPipeline == RenderPipelines.ITEM_TRANSLUCENT ||

            originalRenderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_TRANSLUCENT ||

            originalRenderPipeline == RenderPipelines.END_CRYSTAL_BEAM ||
            originalRenderPipeline == RenderPipelines.BREEZE_WIND ||
            originalRenderPipeline == RenderPipelines.ENERGY_SWIRL ||
            originalRenderPipeline == RenderPipelines.TEXT ||
            originalRenderPipeline == RenderPipelines.TEXT_BACKGROUND ||
            originalRenderPipeline == RenderPipelines.TEXT_POLYGON_OFFSET
        ) {
            alphaCutout = 0.1F;
        }
        else if (
            originalRenderPipeline == RenderPipelines.CUTOUT_BLOCK ||
            originalRenderPipeline == RenderPipelines.CUTOUT_TERRAIN
        ) {
            alphaCutout = 0.5F;
        }

        var fragmentSrcBuilder = new StringBuilder();

        fragmentSrcBuilder.append("#define CANPIPE_MATERIAL_SHADER\n");
        if (alphaCutout != null) {
            fragmentSrcBuilder.append("#define CANPIPE_ALPHA_CUTOUT "+alphaCutout+"\n");
        }
        if (terrain) {
            fragmentSrcBuilder.append("#define CANPIPE_TERRAIN\n");
        }
        if (shadow) {
            fragmentSrcBuilder.append("#define DEPTH_PASS\n");
        }
        if (enablePBR) {
            fragmentSrcBuilder.append("#define PBR_ENABLED\n");
        }
        if (flatVertexColor) {
            fragmentSrcBuilder.append("#define CANPIPE_FLAT_VERTEX_COLOR\n");
        }
        if (hasTexturePos) {
            fragmentSrcBuilder.append("#define CANPIPE_HAS_TEXTURE_POS\n");
        }
        if (hasOverlayPos) {
            fragmentSrcBuilder.append("#define CANPIPE_HAS_OVERLAY_POS\n");
        }
        if (hasMaterialFlags) {
            fragmentSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_FLAGS\n");
        }
        if (hasMaterialIndex) {
            fragmentSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_INDEX\n");
        }
        fragmentSrcBuilder.append(
        """

        #include frex:shaders/api/fragment.glsl
        #include frex:shaders/api/sampler.glsl
        #include frex:shaders/api/material.glsl
        #include frex:shaders/api/view.glsl
        #include frex:shaders/api/header.glsl

        """);
        fragmentSrcBuilder.append(materialsFunctionsSrc);
        fragmentSrcBuilder.append(fragmentSrcOriginal);
        fragmentSrcBuilder.append(
        """

        void main() {
            #if defined CANPIPE_HAS_TEXTURE_POS
                frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);
            #else
                frx_sampleColor = vec4(1.0);
            #endif

            frx_fragEmissive = frx_matEmissive;

            #if !defined DEPTH_PASS
                frx_fragLight = frx_vertexLight;
            #endif

            frx_fragEnableAo = frx_matDisableAo == 0;
            frx_fragEnableDiffuse = frx_matDisableDiffuse == 0;

            frx_fragColor = frx_sampleColor * frx_vertexColor;

            #if defined CANPIPE_ALPHA_CUTOUT
                if (frx_fragColor.a < CANPIPE_ALPHA_CUTOUT) discard;
            #endif

        """);
        fragmentSrcBuilder.append(materialsSwitchSrc);
        fragmentSrcBuilder.append(
        """

            frx_pipelineFragment();
        }
        """);

        return fragmentSrcBuilder.toString();
    }

}
