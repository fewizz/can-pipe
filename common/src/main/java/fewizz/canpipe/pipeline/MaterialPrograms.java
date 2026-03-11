package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.b3d.GpuDeviceExtended;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class MaterialPrograms {

    private MaterialPrograms() {}

    static RenderPipeline load(
        RenderPipeline originalRenderPipeline,
        int glslVersion,
        boolean enablePBR,
        boolean shadow,
        Optional<Integer> shadowMapSize,
        Identifier vertexShaderLocation,
        Identifier fragmentShaderLocation,
        Map<Identifier, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        List<String> samplers,
        Function<Identifier, Optional<String>> getShaderSource,
        float shadowsOffsetSlopeFactor,
        float shadowsOffsetBiasUnits
    ) {
        VertexFormat vertexFormat;
        if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.BLOCK) {
            vertexFormat = CanPipe.VertexFormats.BLOCK;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.ENTITY) {
            vertexFormat = !shadow ? CanPipe.VertexFormats.ENTITY : CanPipe.VertexFormats.ENTITY_SHADOW;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE) {
            vertexFormat = !shadow ? CanPipe.VertexFormats.PARTICLE : CanPipe.VertexFormats.PARTICLE_SHADOW;
        }
        else if (originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.POSITION_COLOR_LIGHTMAP) {
            vertexFormat = CanPipe.VertexFormats.POSITION_COLOR_LIGHTMAP;
        }
        else {
            throw new RuntimeException("Unexpected vertex format to replace: "+originalRenderPipeline.getVertexFormat().toString());
        }

        var renderPipelineBuilder = RenderPipeline.builder();
        {
            Identifier location = Identifier.fromNamespaceAndPath(
                CanPipe.MOD_ID, (!shadow ? "material" : "material_shadow")+"-"+originalRenderPipeline.getLocation().getPath()
            );
            renderPipelineBuilder
                .withLocation(location)
                .withVertexShader(vertexShaderLocation.withSuffix("/"+originalRenderPipeline.getLocation().getPath()))
                .withFragmentShader(fragmentShaderLocation.withSuffix("/"+originalRenderPipeline.getLocation().getPath()))
                .withDepthStencilState(new DepthStencilState(
                    originalRenderPipeline.getDepthStencilState().depthTest(),
                    originalRenderPipeline.getDepthStencilState().writeDepth(),
                    !shadow ? originalRenderPipeline.getDepthStencilState().depthBiasScaleFactor() : shadowsOffsetSlopeFactor,
                    !shadow ? originalRenderPipeline.getDepthStencilState().depthBiasConstant() : shadowsOffsetBiasUnits
                ))
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(!shadow ? originalRenderPipeline.isCull() : false)
                .withColorTargetState(new ColorTargetState(
                    originalRenderPipeline.getColorTargetState().blendFunction(),
                    originalRenderPipeline.getColorTargetState().writeMask()
                ))
                .withVertexFormat(vertexFormat, originalRenderPipeline.getVertexFormatMode());
        }

        renderPipelineBuilder.withUniform("frxu_ub_cascade", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("canpipe_ub_render_target", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("canpipe_ub_origin_type", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("canpipe_ub_is_rendering_hand", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_shadow", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);

        boolean terrain = originalRenderPipeline.getUniforms().stream().anyMatch(u -> u.name().equals("ChunkSection"));

        if (terrain) {
            renderPipelineBuilder.withUniform("ChunkSection", UniformType.UNIFORM_BUFFER);
        }
        else {
            renderPipelineBuilder.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER);
        }
        renderPipelineBuilder.withUniform("Projection", UniformType.UNIFORM_BUFFER);
        renderPipelineBuilder.withUniform("Fog", UniformType.UNIFORM_BUFFER);

        renderPipelineBuilder.withSampler("Sampler0");
        renderPipelineBuilder.withSampler("Sampler1");
        renderPipelineBuilder.withSampler("Sampler2");

        renderPipelineBuilder.withUniform("canpipe_spritesExtents", UniformType.TEXEL_BUFFER, TextureFormat.valueOf("RGBA16_UNORM"));

        for (String sampler : samplers) {
            renderPipelineBuilder.withSampler(sampler);
        }

        var renderPipeline = renderPipelineBuilder.build();

        String vertexSrc = getVertexSrc(vertexShaderLocation, getShaderSource, vertexFormat, originalRenderPipeline, shadow, terrain, enablePBR);
        String fragmentSrc = getFragmentSrc(fragmentShaderLocation, getShaderSource, vertexFormat, originalRenderPipeline, shadow, terrain, enablePBR);

        Function<String, String> postprocess = (String src) -> {
            // These three ideally shouldn't be in a material shader, but it's still possible
            src = src.replaceAll("uniform\\s+ivec2\\s+frxu_size;", "const ivec2 frxu_size = ivec2(-1);");
            src = src.replaceAll("uniform\\s+int\\s+frxu_lod;", "const int frxu_lod = -1;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_layer;", "const int frxu_layer = -1;");

            src = src.replaceAll("uniform\\s+int\\s+frxu_cascade;", "// uniform int frxu_cascade;");
            src =
                "\n"+
                "layout(std140) uniform frxu_ub_cascade {\n"+
                "    int frxu_cascade;\n"+
                "};\n\n"+
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

        ((GpuDeviceExtended) RenderSystem.getDevice()).canpipe_precompilePipelineShaderModules(
            renderPipeline,
            (Identifier location, ShaderType type) -> {
                String src = switch (type) {
                    case ShaderType.VERTEX -> vertexSrc;
                    case ShaderType.FRAGMENT -> fragmentSrc;
                };

                return Shaders.process(
                    location, src, type, glslVersion, options, appliedOptions,
                    getShaderSource, shadowMapSize, postprocess
                );
            },
            (String log, Identifier location, String src) -> {
                Path compilationErrorsPath = CanPipe.getCompilationErrorsDirPath();
                try {
                    Files.createDirectories(compilationErrorsPath);
                    Files.writeString(
                        compilationErrorsPath.resolve(location.toDebugFileName()),
                        src+"\n"+log
                    );
                } catch (IOException e) {
                    CanPipe.LOGGER.warn("Couldn't save \""+location.toString()+"\" compilation error", e);
                }
                throw new RuntimeException("Couldn't compile \""+location.toString()+"\": "+log);
            }
        );

        return renderPipeline;
    }

    private static String getVertexSrc(
        Identifier vertexShaderLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        VertexFormat vertexFormat,
        RenderPipeline originalRenderPipeline,
        boolean shadow,
        boolean terrain,
        boolean enablePBR
    ) {
        String vertexSrcOriginal = getShaderSource.apply(vertexShaderLocation).get();

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = vertexFormat.contains(VertexFormatElement.UV0);
        boolean hasOverlayPos = vertexFormat.contains(VertexFormatElement.UV1);
        boolean hasMaterialFlags = vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

        StringBuilder materialsFunctionsSrc = new StringBuilder();
        StringBuilder materialsSwitchSrc = new StringBuilder();

        if (vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)) {
            materialsSwitchSrc.append("    switch (canpipe_materialIndex) {\n");

            for (Material m : Materials.usedByRenderType(originalRenderPipeline)) {
                String src = shadow ? m.depthVertexShaderSource : m.vertexShaderSource;
                if (src == null) {
                    continue;
                }
                int id = Materials.id(m);
                src = src.replace("frx_materialFragment", "_material_"+id);

                String materialFunctionName = "_material_"+id;

                materialsSwitchSrc.append("        case "+id+": "+materialFunctionName+"(); break;\n");
                materialsFunctionsSrc.append(src.replace("frx_materialVertex", materialFunctionName) + "\n\n");
            }

            materialsSwitchSrc.append("        default: break;\n");
            materialsSwitchSrc.append("    }\n");
        }

        var vertexSrcBuilder = new StringBuilder();

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
        vertexSrcBuilder.append("#include frex:shaders/api/view.glsl\n");
        vertexSrcBuilder.append("#include frex:shaders/api/world.glsl\n");
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
        vertexSrcBuilder.append("in ivec2 in_lightmap;  // UV2\n");
        vertexSrcBuilder.append(
            vertexFormat.contains(VertexFormatElement.NORMAL) ?
            "in vec3 in_normal; // Normal\n" :
            "const vec3 in_normal = vec3(0.0, 1.0, 0.0);  // Normal\n"
        );
        if (hasMaterialFlags) {
            vertexSrcBuilder.append("#define CANPIPE_HAS_MATERIAL_FLAGS\n");
            vertexSrcBuilder.append("in int in_materialFlags;\n");
        }
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormatElements.AO) ?
            "in float in_ao;\n" :
            "const float in_ao = 1.0;\n"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormatElements.SPRITE_INDEX) ?
            "in int in_spriteIndex;\n" :
            "const int in_spriteIndex = -1\n;"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX) ?
            "in int in_materialIndex;\n" :
            "const int in_materialIndex = -1;\n"
        );
        vertexSrcBuilder.append(
            vertexFormat.contains(CanPipe.VertexFormatElements.TANGENT) ?
            "in vec4 in_tangent;\n" :
            "const vec4 in_tangent = vec4(1.0);\n"
        );
        vertexSrcBuilder.append(
        """

        #include frex:shaders/api/vertex.glsl
        #include frex:shaders/api/sampler.glsl
        #include frex:shaders/api/view.glsl
        #include frex:shaders/api/header.glsl

        """
        );
        vertexSrcBuilder.append(materialsFunctionsSrc);
        vertexSrcBuilder.append(vertexSrcOriginal);
        vertexSrcBuilder.append(
        """

        void main() {
            frx_vertex = vec4(in_vertex, 1.0);

            canpipe_spriteIndex = in_spriteIndex;
            canpipe_materialIndex = in_materialIndex;
            #if defined CANPIPE_HAS_MATERIAL_FLAGS
                canpipe_materialFlags = in_materialFlags;
            #endif

            #if defined CANPIPE_HAS_TEXTURE_POS
                if (canpipe_spriteIndex != -1) {
                    canpipe_spriteExtents = texelFetch(canpipe_spritesExtents, canpipe_spriteIndex);
                    frx_texcoord = frx_normalizeMappedUV(in_uv);
                }
                else {
                    canpipe_spriteExtents = vec4(0.0, 0.0, 1.0, 1.0);
                    frx_texcoord = in_uv;
                }
            #endif
            frx_vertexColor = in_color;

            #if !defined DEPTH_PASS
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
        boolean shadow,
        boolean terrain,
        boolean enablePBR
    ) {
        String fragmentSrcOriginal = getShaderSource.apply(fragmentShaderLocation).get();

        StringBuilder materialsFunctionsSrc = new StringBuilder();
        StringBuilder materialsSwitchSrc = new StringBuilder();

        if (vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)) {
            materialsSwitchSrc.append("    switch (canpipe_materialIndex) {\n");

            for (Material m : Materials.usedByRenderType(originalRenderPipeline)) {
                String src = shadow ? m.depthFragmentShaderSource : m.fragmentShaderSource;
                if (src == null) {
                    continue;
                }
                int id = Materials.id(m);
                src = src.replace("frx_materialFragment", "_material_"+id);

                String materialFunctionName = "_material_"+id;

                materialsSwitchSrc.append("        case "+id+": "+materialFunctionName+"(); break;\n");
                materialsFunctionsSrc.append(src.replace("frx_materialFragment", materialFunctionName) + "\n\n");
            }

            materialsSwitchSrc.append("        default: break;\n");
            materialsSwitchSrc.append("    }\n");
        }

        Float alphaCutout = null;
        if (
            originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE ||

            // originalRenderPipeline == RenderPipelines.TRANSLUCENT_MOVING_BLOCK ||
            originalRenderPipeline == RenderPipelines.GLINT ||
            originalRenderPipeline == RenderPipelines.LINES ||
            originalRenderPipeline == RenderPipelines.SECONDARY_BLOCK_OUTLINE ||
            originalRenderPipeline == RenderPipelines.LINES ||
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
            originalRenderPipeline == RenderPipelines.ENERGY_SWIRL
        ) {
            alphaCutout = 0.1F;
        }
        else if (
            originalRenderPipeline == RenderPipelines.CUTOUT_BLOCK ||
            originalRenderPipeline == RenderPipelines.CUTOUT_TERRAIN
        ) {
            alphaCutout = 0.5F;
        }

        boolean flatVertexColor = originalRenderPipeline == RenderPipelines.LEASH;
        boolean hasTexturePos = vertexFormat.contains(VertexFormatElement.UV0);
        boolean hasOverlayPos = vertexFormat.contains(VertexFormatElement.UV1);
        boolean hasMaterialFlags = vertexFormat.contains(CanPipe.VertexFormatElements.MATERIAL_FLAGS);

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
