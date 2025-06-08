package fewizz.canpipe.pipeline;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Streams;
import com.mojang.blaze3d.opengl.GlRenderPipeline;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.opengl.Uniform.Ubo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;

public class MaterialProgram extends ProgramBase {

    static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        new RenderPipeline.UniformDescription("canpipe_ub_material_program", UniformType.UNIFORM_BUFFER)
    );

    static final List<String> INTERNAL_SAMPLER_NAMES = List.of(
        "frxs_baseColor", "frxs_lightmap", "canpipe_spritesExtents"
    );

    public final boolean shadow;
    public final Map<String, GlTextureView> samplerToTexture;

    private MaterialProgram(
        ResourceLocation pipelineLocation, VertexFormat vertexFormat,
        Shader vertexShader, Shader fragmentShader,
        List<String> samplers,
        List<Optional<? extends GlTextureView>> textureViews,
        boolean shadow
    ) {
        super(
            "material-program", vertexFormat,
            Stream.concat(INTERNAL_SAMPLER_NAMES.stream(), samplers.stream()).toList(),
            DEFAULT_UNIFORMS,
            vertexShader, fragmentShader
        );

        if (samplers.size() > textureViews.size()) {
            CanPipe.LOGGER.warn("Material program has more samplers than textures");
        }
        if (samplers.size() < textureViews.size()) {
            CanPipe.LOGGER.warn("Material program has less samplers than textures");
        }

        Map<String, GlTextureView> samplerToTexture = new HashMap<>();
        for (int i = 0; i < Math.min(samplers.size(), textureViews.size()); ++i) {
            String sampler = samplers.get(i);
            Optional<? extends GlTextureView> texture = textureViews.get(i);
            if (texture.isEmpty()) {
                if (this.getUniform(sampler) != null) {
                    throw new NullPointerException("Couldn't find texture for sampler \""+sampler+"\"");
                }
                this.getUniforms().remove(sampler);
            }
            else {
                samplerToTexture.put(sampler, texture.get());
            }
            samplerToTexture.put(sampler, texture.get());
        }

        this.samplerToTexture = Collections.unmodifiableMap(samplerToTexture);
        this.shadow = shadow;
    }

    public static GlRenderPipeline load(
        ResourceLocation pipelineLocation,
        RenderPipeline originalRenderPipeline,
        int glslVersion,
        boolean enablePBR,
        boolean depthPass,
        @Nullable Framebuffer shadowFramebuffer,
        ResourceLocation vertexShaderLocation,
        ResourceLocation fragmentShaderLocation,
        Map<ResourceLocation, Option> options,
        Map<Option.Element<?>, Object> appliedOptions,
        List<String> samplers,
        List<Optional<? extends GlTextureView>> textureViews,
        Function<ResourceLocation, Optional<String>> getShaderSource,
        float shadowsOffsetSlopeFactor,
        float shadowsOffsetBiasUnits
    ) {
        if (shadowFramebuffer != null && shadowFramebuffer.depthAttachment != null) {
            var depthArray = shadowFramebuffer.depthAttachment.texture().view;
            samplers = Streams.concat(
                samplers.stream(),
                List.of("frxs_shadowMap", "frxs_shadowMapTexture").stream()
            ).toList();
            textureViews = Streams.concat(
                textureViews.stream(),
                List.of(Optional.of(depthArray), Optional.of(depthArray)).stream()
            ).toList();
        }

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
        else {
            throw new RuntimeException(originalRenderPipeline.getVertexFormat().toString());
        }

        float alphaCutout;
        if (
            originalRenderPipeline.getVertexFormat() == DefaultVertexFormat.PARTICLE ||

            // use ITEM_ENTITY_TARGET output state shard
            originalRenderPipeline == RenderPipelines.TRANSLUCENT_MOVING_BLOCK ||
            originalRenderPipeline == RenderPipelines.LINE_STRIP ||
            originalRenderPipeline == RenderPipelines.SECONDARY_BLOCK_OUTLINE ||
            originalRenderPipeline == RenderPipelines.GLINT ||
            originalRenderPipeline == RenderPipelines.LINES ||

            originalRenderPipeline == RenderPipelines.CUTOUT ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT ||
            originalRenderPipeline == RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE ||
            originalRenderPipeline == RenderPipelines.ARMOR_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL ||
            originalRenderPipeline == RenderPipelines.ARMOR_TRANSLUCENT
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

        vertexSrc =
            "#define CANPIPE_MATERIAL_SHADER\n"+
            (depthPass ? "#define DEPTH_PASS\n" : "")+
            "\n"+
            "#include canpipe:shaders/uniform_blocks.glsl\n"+
            "\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.POSITION)+") in vec3 in_vertex;  // Position\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.COLOR)+") in vec4 in_color;  // Color\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV0)+") in vec2 in_uv;  // UV0\n"+
            (
                vertexFormat.contains(VertexFormatElement.UV1) ?
                "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV1)+") in ivec2 in_uv1" :
                "const ivec2 in_v1 = ivec2(0)"
            ) + ";\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.UV2)+") in ivec2 in_lightmap;  // UV2\n"+
            (
                vertexFormat.contains(VertexFormatElement.NORMAL) ?
                "layout(location = "+vertexFormat.getElements().indexOf(VertexFormatElement.NORMAL)+") in vec3 in_normal" :
                "const vec3 in_normal = vec3(0.0, 1.0, 0.0)"
            ) + ";  // Normal\n"+
            "layout(location = "+vertexFormat.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_FLAGS)+") in int in_materialFlags;\n"+
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
                frx_vertexTangent = in_tangent;
                canpipe_spriteIndex = in_spriteIndex;
                canpipe_materialIndex = in_materialIndex;
                canpipe_materialFlags = in_materialFlags;

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
                frx_sampleColor = texture(frxs_baseColor, frx_texcoord, frx_matUnmipped * -4.0);
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

        Function<String, String> postProcess = (String s) -> {
            return s.replaceAll("uniform\\s+int\\s+frxu_cascade;", "// uniform int frxu_cascade;");
        };

        var vertexShader = Shader.load(
            vertexShaderLocation, vertexSrc, ShaderType.VERTEX, glslVersion,
            options, appliedOptions, getShaderSource, shadowFramebuffer, postProcess
        );
        var fragmentShader = Shader.load(
            fragmentShaderLocation, fragmentSrc, ShaderType.FRAGMENT, glslVersion,
            options, appliedOptions, getShaderSource, shadowFramebuffer, postProcess
        );

        var materialProgram = new MaterialProgram(
            pipelineLocation, vertexFormat,
            vertexShader, fragmentShader,
            samplers, textureViews, depthPass
        );

        var renderPipelineBuilder = RenderPipeline.builder();
        if (!depthPass) {
            renderPipelineBuilder
                .withLocation(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                .withVertexShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                .withFragmentShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material"))
                .withDepthTestFunction(originalRenderPipeline.getDepthTestFunction())
                .withDepthBias(originalRenderPipeline.getDepthBiasScaleFactor(), originalRenderPipeline.getDepthBiasConstant())
                .withPolygonMode(originalRenderPipeline.getPolygonMode())
                .withCull(originalRenderPipeline.isCull())
                .withColorWrite(originalRenderPipeline.isWriteColor(), originalRenderPipeline.isWriteAlpha())
                .withDepthWrite(originalRenderPipeline.isWriteDepth())
                .withVertexFormat(vertexFormat, originalRenderPipeline.getVertexFormatMode());
        }
        else {
            renderPipelineBuilder
                .withLocation(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
                .withVertexShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
                .withFragmentShader(ResourceLocation.fromNamespaceAndPath("canpipe", "material-shadow"))
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
        for (var u : materialProgram.getUniforms().entrySet()) {
            if (u.getValue() instanceof Ubo) {
                renderPipelineBuilder.withUniform(u.getKey(), UniformType.UNIFORM_BUFFER);
            }
        }

        var renderPipeline = renderPipelineBuilder.build();

        return new GlRenderPipeline(renderPipeline, materialProgram);
    }

}
