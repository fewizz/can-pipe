package fewizz.canpipe.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.renderpearl.RenderPipelineBuilderExtended;
import fewizz.canpipe.renderpearl.mixin.FrontendGpuDeviceAccessor;
import fewizz.canpipe.renderpearl.mixin.PipelineBuilderAccessor;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
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
        List<VertexFormat> vertexBindings = new ArrayList<>();
        if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.BLOCK) {
            vertexBindings.add(CanPipe.VertexFormats.BLOCK);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.ENTITY) {
            vertexBindings.add(!shadow ? CanPipe.VertexFormats.ENTITY : CanPipe.VertexFormats.ENTITY_SHADOW);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.ENTITY_GLINT_SPECIAL) {
            vertexBindings.add(CanPipe.VertexFormats.ENTITY_GLINT_SPECIAL);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.PARTICLE) {
            vertexBindings.add(!shadow ? CanPipe.VertexFormats.PARTICLE : CanPipe.VertexFormats.PARTICLE_SHADOW);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_COLOR_LIGHTMAP) {
            vertexBindings.add(CanPipe.VertexFormats.POSITION_COLOR_LIGHTMAP);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP) {
            vertexBindings.add(CanPipe.VertexFormats.POSITION_COLOR_TEX_LIGHTMAP);
        }
        else if (originalRenderPipeline.getVertexFormatBinding(0) == DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR) {
            vertexBindings.add(CanPipe.VertexFormats.POSITION_TEX_LIGHTMAP_COLOR);
        }
        else {
            throw new RuntimeException("Unexpected vertex format to replace: "+originalRenderPipeline.getVertexFormatBinding(0).toString());
        }

        for (int bindingIndex = 1; bindingIndex < originalRenderPipeline.getVertexFormatBindings().size(); ++bindingIndex) {
            vertexBindings.add(originalRenderPipeline.getVertexFormatBindings().get(bindingIndex));
        }

        Identifier location = Identifier.fromNamespaceAndPath(
            CanPipe.MOD_ID, (!shadow ? "material" : "material_shadow")+"-"+originalRenderPipeline.getLocation().getPath()
        );

        var renderPipelineBuilder = RenderPipeline.builder();
        {
            renderPipelineBuilder
                .withLocation(location)
                .withVertexShader(vertexShaderLocation)//vertexShaderIDWithPostfix)
                .withFragmentShader(fragmentShaderLocation)//fragmentShaderIDWithPostfix)
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(!shadow ? originalRenderPipeline.isCull() : false)
                .withPrimitiveTopology(originalRenderPipeline.getPrimitiveTopology());

            for (int bindingIndex = 0; bindingIndex < vertexBindings.size(); ++bindingIndex) {
                renderPipelineBuilder.withVertexBinding(bindingIndex, vertexBindings.get(bindingIndex));
            }

            ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_allowNoColorTargets();

            var dsState = originalRenderPipeline.getDepthStencilState();
            if (dsState != null) {
                if (!awareOfDepthRangeChanges || shadow) {
                    dsState = new DepthStencilState(
                        CanPipe.reverseCompareOp(dsState.depthTest()),
                        dsState.writeDepth(),
                        !shadow ? -dsState.depthBiasScaleFactor() : shadowsOffsetSlopeFactor,
                        !shadow ? -dsState.depthBiasConstant() : shadowsOffsetBiasUnits
                    );
                }

                renderPipelineBuilder.withDepthStencilState(dsState);
            }
        }

        List<String> attributeNames = vertexBindings.stream()
            .flatMap((VertexFormat v) -> v == null ? List.<VertexFormatElement>of().stream() : v.getElements().stream())
            .map((VertexFormatElement ve) -> ve.name())
            .toList();

        boolean terrain = originalRenderPipeline.getBindGroupLayouts().stream().anyMatch(bgl -> bgl == BindGroupLayouts.TERRAIN_INFO);
        boolean terrainMultidraw = terrain && !originalRenderPipeline.getBindGroupLayouts().stream().anyMatch(bgl -> bgl == BindGroupLayouts.CHUNK_SECTION);
        boolean hasOverlayPos = attributeNames.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);

        if (terrain) {
            if (!terrainMultidraw) {
                renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.CHUNK_SECTION);
            }
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.TERRAIN_INFO);
        }
        else {
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
        }
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.PROJECTION);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.FOG);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.GLOBALS);

        if (hasOverlayPos) {
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER1_SAMPLER2);
        }
        else {
            renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2);
        }

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

        for (var externalUboName : Uniforms.EXTERNAL_UBOS.keySet()) {
            bindGroupLayoutBuilder.withUniform(externalUboName, UniformType.UNIFORM_BUFFER);
        }

        for (String sampler : samplers) {
            bindGroupLayoutBuilder.withUniform(sampler, UniformType.COMBINED_IMAGE_SAMPLER);
        }
        renderPipelineBuilder.withBindGroupLayout(bindGroupLayoutBuilder.build());

        Collection<Material> materials;

        if (Set.of(RenderPipelines.SOLID_BLOCK, RenderPipelines.SOLID_TERRAIN, RenderPipelines.SOLID_TERRAIN_MULTIDRAW).contains(originalRenderPipeline)) {
            materials = MaterialMaps.getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer.SOLID);
        }
        else if (Set.of(RenderPipelines.CUTOUT_BLOCK, RenderPipelines.CUTOUT_TERRAIN, RenderPipelines.CUTOUT_TERRAIN_MULTIDRAW).contains(originalRenderPipeline)) {
            materials = MaterialMaps.getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer.CUTOUT);
        }
        else if (Set.of(RenderPipelines.TRANSLUCENT_BLOCK, RenderPipelines.TRANSLUCENT_TERRAIN, RenderPipelines.TRANSLUCENT_TERRAIN_MULTIDRAW).contains(originalRenderPipeline)) {
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

        // TODO
        /*TriConsumer<String, Identifier, String> onCompilationError = (String log, Identifier shaderLocation, String src) -> {
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
        };*/

        String vertexSrc = getVertexSrc(vertexShaderLocation, getShaderSource, attributeNames, originalRenderPipeline, materials, shadow, terrain, terrainMultidraw, enablePBR);
        String fragmentSrc = getFragmentSrc(fragmentShaderLocation, getShaderSource, attributeNames, originalRenderPipeline, materials, shadow, terrain, terrainMultidraw, enablePBR);

        // TODO
        /*((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompileShaderModule(
            vertexShaderIDWithPostfix,
            Shaders.process(
                vertexShaderLocation, vertexSrc, ShaderType.VERTEX, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ),
            ShaderType.VERTEX,
            onCompilationError
        );
        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompileShaderModule(
            fragmentShaderIDWithPostfix,
            Shaders.process(
                fragmentShaderLocation, fragmentSrc, ShaderType.FRAGMENT, glslVersion, options, appliedOptions,
                getShaderSource, shadowMapSize, postprocess
            ),
            ShaderType.FRAGMENT,
            onCompilationError
        );*/
        // (RenderSystem.getDevice()).compilePipeline(renderPipeline, null, null)

        if (!awareOfDepthRangeChanges && RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()) {
            renderPipelineBuilder.withShaderDefine("CANPIPE_CLIP_DEPTH_RANGE_PATCH");
        }

        RenderPipeline exampleRenderPipeline = renderPipelineBuilder.build();

        var pipelineBuilder = ((FrontendGpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getPipelineBuilder();

        ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaderCreateInfos = new ReferenceArrayList<>();
        Object2IntOpenHashMap<String> uniformBindings = new Object2IntOpenHashMap<>();

        var renderPipelineCreateInfo = ((PipelineBuilderAccessor) pipelineBuilder).canpipe_invokeGenerateBackendCreateInfo(
            exampleRenderPipeline,
            new ShaderSource() {

                @Override
                public @Nullable String getShader(Identifier id, ShaderType type) {
                    Identifier location;
                    String src;
                    if (type == ShaderType.VERTEX) {
                        if (id != vertexShaderLocation) {
                            throw new RuntimeException("Expected vertex shader location to be "+vertexShaderLocation+", but got "+id);
                        }
                        location = vertexShaderLocation;
                        src = vertexSrc;
                    }
                    else if (type == ShaderType.FRAGMENT) {
                        if (id != fragmentShaderLocation) {
                            throw new RuntimeException("Expected fragment shader location to be "+fragmentShaderLocation+", but got "+id);
                        }
                        location = fragmentShaderLocation;
                        src = fragmentSrc;
                    }
                    else {
                        throw new NotImplementedException();
                    }

                    return Shaders.process(
                        location, src, type, glslVersion, options, appliedOptions,
                        getShaderSource, shadowMapSize, postprocess
                    );
                }

                @Override
                public @Nullable CachedIncludeSource getInclude(Identifier id) { return null; }

                @Override
                public void close() {}
                
            },
            shaderCreateInfos,
            uniformBindings
        );

        return new MaterialProgramLoader(location, renderPipelineBuilder, originalRenderPipeline, renderPipelineCreateInfo, shaderCreateInfos, uniformBindings);
    }

    private static String getVertexSrc(
        Identifier vertexShaderLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        List<String> attributeNames,
        RenderPipeline originalRenderPipeline,
        Collection<Material> materials,
        boolean shadow,
        boolean terrain,
        boolean terrainMultidraw,
        boolean enablePBR
    ) {
        String vertexSrcOriginal = getShaderSource.apply(vertexShaderLocation).get();

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = attributeNames.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        boolean hasOverlayPos = attributeNames.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);
        boolean hasLightmapPos = attributeNames.contains(DefaultVertexFormat.UV2_SEMANTIC_NAME);
        boolean hasMaterialFlags = attributeNames.contains(CanPipe.VertexFormats.MATERIAL_FLAGS_ATTRIBUTE_NAME);
        boolean hasMaterialIndex = attributeNames.contains(CanPipe.VertexFormats.MATERIAL_INDEX_ATTRIBUTE_NAME);

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
        if (terrainMultidraw) {
            vertexSrcBuilder.append("#define CANPIPE_TERRAIN_MULTIDRAW\n");
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
        vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("Position")+") in vec3 in_vertex;  // Position\n");
        vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("Color")+") in vec4 in_color;  // Color\n");
        if (hasTexturePos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_TEXTURE_POS\n");
            vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("UV0")+") in vec2 in_uv;  // UV0\n");
        }
        if (hasOverlayPos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_OVERLAY_POS\n");
            vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("UV1")+") in ivec2 in_overlayPos;  // UV1\n");
        }
        if (hasLightmapPos) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_LIGHTMAP_POS\n");
            vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("UV2")+") in ivec2 in_lightmapPos;  // UV2\n");
        }
        if (terrainMultidraw) {
            vertexSrcBuilder.append(
                "layout(location = "+attributeNames.indexOf("ChunkPosition")+") in ivec3 ChunkPosition;\n"+
                "layout(location = "+attributeNames.indexOf("ChunkVisibility")+") in float ChunkVisibility;\n"
            );
        }
        if (hasMaterialFlags) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_FLAGS\n");
            vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("in_materialFlags")+") in int in_materialFlags;\n");
        }
        if (hasMaterialIndex) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_INDEX\n");
            vertexSrcBuilder.append("layout(location = "+attributeNames.indexOf("in_materialIndex")+") in int in_materialIndex;\n");
        }
        vertexSrcBuilder.append(
            attributeNames.contains(DefaultVertexFormat.NORMAL_SEMANTIC_NAME) ?
            "layout(location = "+attributeNames.indexOf("Normal")+") in vec3 in_normal; // Normal\n" :
            "const vec3 in_normal = vec3(0.0, 1.0, 0.0);  // Normal\n"
        );
        vertexSrcBuilder.append(
            attributeNames.contains(CanPipe.VertexFormats.AO_ATTRIBUTE_NAME) ?
            "layout(location = "+attributeNames.indexOf("in_ao")+") in float in_ao;\n" :
            "const float in_ao = 1.0;\n"
        );
        vertexSrcBuilder.append(
            attributeNames.contains(CanPipe.VertexFormats.SPRITE_INDEX_ATTRIBUTE_NAME) ?
            "layout(location = "+attributeNames.indexOf("in_spriteIndex")+") in int in_spriteIndex;\n" :
            "const int in_spriteIndex = -1;\n"
        );
        vertexSrcBuilder.append(
            attributeNames.contains(CanPipe.VertexFormats.TANGENT_ATTRIBUTE_NAME) ?
            "layout(location = "+attributeNames.indexOf("in_tangent")+") in vec4 in_tangent;\n" :
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

            #if defined CANPIPE_TERRAIN_MULTIDRAW
                canpipe_chunkVisibility = ChunkVisibility;
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
        List<String> attributeNames,
        RenderPipeline originalRenderPipeline,
        Collection<Material> materials,
        boolean shadow,
        boolean terrain,
        boolean terrainMultidraw,
        boolean enablePBR
    ) {
        String fragmentSrcOriginal = getShaderSource.apply(fragmentShaderLocation).get();

        StringBuilder materialsFunctionsSrc = new StringBuilder();
        StringBuilder materialsSwitchSrc = new StringBuilder();

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = attributeNames.contains(DefaultVertexFormat.UV0_SEMANTIC_NAME);
        boolean hasOverlayPos = attributeNames.contains(DefaultVertexFormat.UV1_SEMANTIC_NAME);
        boolean hasMaterialFlags = attributeNames.contains(CanPipe.VertexFormats.MATERIAL_FLAGS_ATTRIBUTE_NAME);
        boolean hasMaterialIndex = attributeNames.contains(CanPipe.VertexFormats.MATERIAL_INDEX_ATTRIBUTE_NAME);

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
        var alphaCoutputValueStr = originalRenderPipeline.getShaderDefines().values().get("ALPHA_CUTOUT");
        var alphaCoutputValue = alphaCoutputValueStr != null ? Float.parseFloat(alphaCoutputValueStr) : null;

        if (
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_BLOCK ||
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_TERRAIN ||
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_TERRAIN_MULTIDRAW
        ) {
            alphaCutout = 0.01F;
        }
        else if (
            (alphaCoutputValue != null && alphaCoutputValue == 0.1F) ||
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
            originalRenderPipeline == RenderPipelines.ITEM_CUTOUT_GLINT ||
            originalRenderPipeline == RenderPipelines.ITEM_CUTOUT_GLINT_SPECIAL ||
            originalRenderPipeline == RenderPipelines.ITEM_TRANSLUCENT ||
            originalRenderPipeline == RenderPipelines.ITEM_TRANSLUCENT_GLINT ||
            originalRenderPipeline == RenderPipelines.ITEM_TRANSLUCENT_GLINT_SPECIAL ||

            originalRenderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||

            originalRenderPipeline == RenderPipelines.END_CRYSTAL_BEAM ||
            originalRenderPipeline == RenderPipelines.BREEZE_WIND ||
            originalRenderPipeline == RenderPipelines.ENERGY_SWIRL ||
            originalRenderPipeline == RenderPipelines.TEXT ||
            originalRenderPipeline == RenderPipelines.TEXT_POLYGON_OFFSET
        ) {
            alphaCutout = 0.1F;
        }
        else if (
            (alphaCoutputValue != null && alphaCoutputValue == 0.5F) ||
            originalRenderPipeline == RenderPipelines.CUTOUT_BLOCK ||
            originalRenderPipeline == RenderPipelines.CUTOUT_TERRAIN ||
            originalRenderPipeline == RenderPipelines.CUTOUT_TERRAIN_MULTIDRAW
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
        if (terrainMultidraw) {
            fragmentSrcBuilder.append("#define CANPIPE_TERRAIN_MULTIDRAW\n");
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
