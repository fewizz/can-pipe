package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import fewizz.canpipe.mixininterface.DeviceExtended;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public class MaterialPrograms {

    public static RenderPipeline load(
        RenderPipeline originalRenderPipeline,
        int glslVersion,
        boolean enablePBR,
        boolean depthPass,
        Optional<Integer> shadowMapSize,
        ResourceLocation vertexShaderLocation,
        ResourceLocation fragmentShaderLocation,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        List<String> samplers,
        Function<ResourceLocation, Optional<String>> getShaderSource,
        float shadowsOffsetSlopeFactor,
        float shadowsOffsetBiasUnits
    ) {
        String vertexSrc = getShaderSource.apply(vertexShaderLocation).get();
        String fragmentSrc = getShaderSource.apply(fragmentShaderLocation).get();

        VertexFormat vertexFormat;
        if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.BLOCK) {
            vertexFormat = CanPipe.VertexFormats.BLOCK;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.NEW_ENTITY) {
            vertexFormat = CanPipe.VertexFormats.NEW_ENTITY;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE) {
            vertexFormat = CanPipe.VertexFormats.PARTICLE;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.POSITION_COLOR_LIGHTMAP) {
            vertexFormat = originalRenderPipeline.getVertexFormat();  // leave as is
        }
        else {
            throw new RuntimeException("Unexpected vertex format to replace: "+originalRenderPipeline.getVertexFormat().toString());
        }

        float alphaCutout;
        if (
            originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE ||

            // use ITEM_ENTITY_TARGET output state shard
            originalRenderPipeline == RenderPipelines.ITEM_ENTITY_TRANSLUCENT_CULL ||
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_MOVING_BLOCK ||
            originalRenderPipeline == RenderPipelines.GLINT ||
            originalRenderPipeline == RenderPipelines.LINES ||
            originalRenderPipeline == RenderPipelines.SECONDARY_BLOCK_OUTLINE ||
            originalRenderPipeline == RenderPipelines.LINE_STRIP ||

            originalRenderPipeline == RenderPipelines.CUTOUT ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE ||
            originalRenderPipeline == RenderPipelines.ENTITY_SMOOTH_CUTOUT ||
            originalRenderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_TRANSLUCENT ||
            originalRenderPipeline == RenderPipelines.TRIPWIRE ||
            originalRenderPipeline == RenderPipelines.BREEZE_WIND ||
            originalRenderPipeline == RenderPipelines.ENERGY_SWIRL
        ) {
            alphaCutout = 0.1F;
        }
        else if (originalRenderPipeline == RenderPipelines.CUTOUT_MIPPED) {
            alphaCutout = 0.5F;
        }
        else {
            alphaCutout = 0.0F;
        }

        String materialsVertexSrc = "";
        IntList usedMaterialIDs = new IntArrayList();
        for (Material m : Materials.allCopy()) {
            String src = depthPass ? m.depthVertexShaderSource : m.vertexShaderSource;
            if (src == null) {
                continue;
            }
            int id = Materials.id(m);
            src = src.replace("frx_materialVertex", "_material_"+id);
            materialsVertexSrc += src + "\n\n";
            usedMaterialIDs.add(id);
        }

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;

        boolean hasTexturePos = vertexFormat.contains(VertexFormatElement.UV0);
        boolean hasOverlayPos = vertexFormat.contains(VertexFormatElement.UV1);
        boolean hasMaterialFlags = vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

        vertexSrc =
            "#define CANPIPE_MATERIAL_SHADER\n"+
            (depthPass ? "#define DEPTH_PASS\n" : "")+
            (flatVertexColor ? "#define CANPIPE_FLAT_VERTEX_COLOR\n" : "")+
            "\n"+
            "#include canpipe:shaders/uniform_blocks.glsl\n"+
            "\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.POSITION)+") in vec3 in_vertex;  // Position\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.COLOR)+") in vec4 in_color;  // Color\n"+
            (
                hasTexturePos ?
                "#define CANPIPE_HAS_TEXTURE_POS\n"+
                "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV0)+") in vec2 in_uv;  // UV0\n" :
                ""
            ) +
            (
                hasOverlayPos ?
                "#define CANPIPE_HAS_OVERLAY_POS\n"+
                "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV1)+") in ivec2 in_overlayPos;  // UV1\n" :
                ""
            ) +
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV2)+") in ivec2 in_lightmap;  // UV2\n"+
            (
                vertexFormat.contains(VertexFormatElement.NORMAL) ?
                "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.NORMAL)+") in vec3 in_normal" :
                "const vec3 in_normal = vec3(0.0, 1.0, 0.0)"
            ) + ";  // Normal\n"+
            (
                hasMaterialFlags ?
                "#define CANPIPE_MATERIAL_FLAGS\n"+
                "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_FLAGS)+") in int in_materialFlags;\n" :
                ""
            ) +
            (
                vertexFormat.contains(CanPipe.VertexFormatElements.AO) ?
                "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.AO)+") in float in_ao" :
                "const float in_ao = 1.0"
            ) + ";\n"+
            (
                vertexFormat.contains(CanPipe.VertexFormatElements.SPRITE_INDEX) ?
                "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.SPRITE_INDEX)+") in int in_spriteIndex" :
                "const int in_spriteIndex = -1"
            ) + ";\n"+
            (
                vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX) ?
                "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_INDEX)+") in int in_materialIndex" :
                "const int in_materialIndex = -1"
            ) + ";\n"+
            (
                vertexFormat.contains(CanPipe.VertexFormatElements.TANGENT) ?
                "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.TANGENT)+") in vec4 in_tangent"
                : "const vec4 in_tangent = vec4(1.0)"
            ) + ";\n"+
            """

            #include frex:shaders/api/vertex.glsl
            #include frex:shaders/api/view.glsl

            """ +
            materialsVertexSrc +
            vertexSrc +
            """

            void main() {
                frx_vertex = vec4(in_vertex, 1.0);
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
                frx_vertexTangent = in_tangent;
                canpipe_spriteIndex = in_spriteIndex;
                canpipe_materialIndex = in_materialIndex;

                #if defined CANPIPE_HAS_TEXTURE_POS
                    frx_texcoord = in_uv;
                #endif

                #if defined CANPIPE_HAS_MATERIAL_FLAGS
                    canpipe_materialFlags = in_materialFlags;
                #endif

                #if defined CANPIPE_HAS_OVERLAY_POS
                    canpipe_overlayPos = in_overlayPos;
                #endif

                if (frx_isGui && !frx_isHand) {
                    frx_vertexNormal.y *= -1.0;  // compat
                }

            """+
            "    switch (in_materialIndex) {\n" +
            usedMaterialIDs.intStream().mapToObj(id ->
            "        case "+id+": _material_"+id+"(); break;\n"
            ).collect(Collectors.joining()) +
            "        default: break;\n"+
            "    }\n\n"+
            """
                frx_pipelineVertex();
            }
            """;

        String materialsFragmentSrc = "";
        usedMaterialIDs.clear();
        for (Material m : Materials.allCopy()) {
            String src = depthPass ? m.depthFragmentShaderSource : m.fragmentShaderSource;
            if (src == null) {
                continue;
            }
            int id = Materials.id(m);
            src = src.replace("frx_materialFragment", "_material_"+id);
            materialsFragmentSrc += src + "\n\n";
            usedMaterialIDs.add(id);
        }

        fragmentSrc =
            "#extension GL_ARB_conservative_depth: enable\n\n"+
            "#define CANPIPE_MATERIAL_SHADER\n"+
            (depthPass ? "#define DEPTH_PASS\n" : "")+
            (enablePBR ? "#define PBR_ENABLED\n" : "")+
            "#define CANPIPE_ALPHA_CUTOUT "+alphaCutout+"\n"+
            (flatVertexColor ? "#define CANPIPE_FLAT_VERTEX_COLOR\n" : "")+
            (hasTexturePos ? "#define CANPIPE_HAS_TEXTURE_POS\n" : "")+
            (hasOverlayPos ? "#define CANPIPE_HAS_OVERLAY_POS\n" : "")+
            (hasMaterialFlags ? "#define CANPIPE_HAS_MATERIAL_FLAGS\n" : "")+
            """

            layout (depth_unchanged) out float gl_FragDepth;

            #include frex:shaders/api/fragment.glsl
            #include frex:shaders/api/sampler.glsl
            #include frex:shaders/api/material.glsl
            #include frex:shaders/api/view.glsl

            """ +
            materialsFragmentSrc +
            fragmentSrc +
            """

            void main() {
                #if defined CANPIPE_HAS_TEXTURE_POS
                    frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);
                #else
                    frx_sampleColor = vec4(1.0);
                #endif

                frx_fragEmissive = frx_matEmissive;
                frx_fragLight = frx_vertexLight;
                frx_fragEnableAo = frx_matDisableAo == 0;
                frx_fragEnableDiffuse = frx_matDisableDiffuse == 0;

                #if defined PBR_ENABLED
                    // TODO?
                #endif

                frx_fragColor = frx_sampleColor * frx_vertexColor;

                if (frx_fragColor.a < CANPIPE_ALPHA_CUTOUT) {
                    discard;
                }

                """+
            "    switch (canpipe_materialIndex) {\n" +
            usedMaterialIDs.intStream().mapToObj(id ->
            "        case "+id+": _material_"+id+"(); break;\n"
            ).collect(Collectors.joining()) +
            "        default: break;\n"+
            "    }\n\n"+
            """

                frx_pipelineFragment();
            }
            """;

        var renderPipelineBuilder = RenderPipeline.builder();
        if (!depthPass) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("canpipe", "material-"+originalRenderPipeline.getLocation().getPath());
            renderPipelineBuilder
                .withLocation(location)
                .withVertexShader(location)
                .withFragmentShader(location)
                .withDepthTestFunction(originalRenderPipeline.getDepthTestFunction())
                .withDepthBias(originalRenderPipeline.getDepthBiasScaleFactor(), originalRenderPipeline.getDepthBiasConstant())
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(originalRenderPipeline.isCull())
                .withColorWrite(originalRenderPipeline.isWriteColor(), originalRenderPipeline.isWriteAlpha())
                .withDepthWrite(originalRenderPipeline.isWriteDepth())
                .withVertexFormat(vertexFormat, originalRenderPipeline.getVertexFormatMode());
        }
        else {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath("canpipe", "material_shadow-"+originalRenderPipeline.getLocation().getPath());
            renderPipelineBuilder
                .withLocation(location)
                .withVertexShader(location)
                .withFragmentShader(location)
                .withDepthTestFunction(originalRenderPipeline.getDepthTestFunction())
                .withDepthBias(shadowsOffsetSlopeFactor, shadowsOffsetBiasUnits)
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(false)  // Light can pass through chunk edge. Not ideal solution
                .withColorWrite(originalRenderPipeline.isWriteColor(), originalRenderPipeline.isWriteAlpha())
                .withDepthWrite(originalRenderPipeline.isWriteDepth())
                .withVertexFormat(vertexFormat, originalRenderPipeline.getVertexFormatMode());
        }

        if (originalRenderPipeline.getBlendFunction().isPresent()) {
            renderPipelineBuilder.withBlend(originalRenderPipeline.getBlendFunction().get());
        }

        renderPipelineBuilder.withUniform("canpipe_ub_material_program", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("Projection", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("Fog", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withSampler("frxs_baseColor");
        renderPipelineBuilder.withSampler("canpipe_overlay");
        renderPipelineBuilder.withSampler("frxs_lightmap");
        renderPipelineBuilder.withSampler("canpipe_spritesExtents");

        for (String sampler : samplers) {
            renderPipelineBuilder.withSampler(sampler);
        }

        var renderPipeline = renderPipelineBuilder.build();

        final String vertexSrcFinal = vertexSrc;
        final String fragmentSrcFinal = fragmentSrc;

        var device = RenderSystem.getDevice();
        ((DeviceExtended) device).canpipe_compilePipeline(
            renderPipeline,
            (ResourceLocation location, ShaderType type) -> {
                return type == ShaderType.VERTEX ? vertexSrcFinal : fragmentSrcFinal;
            },
            (ResourceLocation location, String source, ShaderType type) -> {
                return Shaders.preprocess(location, source, type, glslVersion, options, appliedOptions, getShaderSource, shadowMapSize, (String s) -> {
                    s = s.replaceAll("uniform\\s+int\\s+frxu_cascade;", "// uniform int frxu_cascade;");
                    s =
                        "#define mc_ub_dynamic_transforms DynamicTransforms\n"+
                        "#define mc_ub_projection Projection\n"+
                        "#define mc_ub_fog Fog\n"+
                        "\n"+
                        s;
                    return s;
                });
            },
            (String error) -> {
                throw new RuntimeException(error);
            }
        );

        return renderPipeline;
    }

}
