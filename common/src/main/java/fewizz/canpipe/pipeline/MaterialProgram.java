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
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.UniformBuffer.FloatUniform;
import fewizz.canpipe.UniformBuffer.IntUniform;
import fewizz.canpipe.UniformBuffer.Vec3Uniform;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.Materials;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.resources.ResourceLocation;

public class MaterialProgram extends ProgramBase {

    static final List<RenderPipeline.UniformDescription> DEFAULT_UNIFORMS = List.of(
        new RenderPipeline.UniformDescription("canpipe_ub_material_program", UniformType.UNIFORM_BUFFER)
    );

    static final List<String> INTERNAL_SAMPLER_NAMES = List.of(
        "frxs_baseColor", "frxs_lightmap", "canpipe_spritesExtents"
    );

    public final Uniform FRX_UB_MATERIAL;

    public static final IntUniform FRXU_CASCADE = new IntUniform();
    public static final IntUniform CANPIPE_RENDER_TARGET = new IntUniform();
    public static final FloatUniform CANPIPE_ALPHA_CUTOUT = new FloatUniform();
    public static final Vec3Uniform CANPIPE_LIGHT_0_DIRECTION = new Vec3Uniform();
    public static final Vec3Uniform CANPIPE_LIGHT_1_DIRECTION = new Vec3Uniform();

    public final VertexFormat vertexFormat;
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
            /*if (texture.isEmpty()) {
                if (this.getUniform(sampler) != null) {
                    throw new NullPointerException("Couldn't find texture for sampler \""+sampler+"\"");
                }
                this.getSamplers().remove(sampler);
            }
            else {
                samplerToTexture.put(sampler, texture.get());
            }*/
            samplerToTexture.put(sampler, texture.get());
        }

        this.samplerToTexture = Collections.unmodifiableMap(samplerToTexture);
        this.vertexFormat = vertexFormat;
        this.shadow = shadow;

        this.FRX_UB_MATERIAL = this.getUniform("frx_ub_material");
    }

    @Override
    public Uniform getUniform(String name) {
        if (name.equals("Light0_Direction")) { name = "canpipe_light0Direction"; }
        if (name.equals("Light1_Direction")) { name = "canpipe_light1Direction"; }
        return super.getUniform(name);
    }

    /*@Override
    public void setDefaultUniforms(Mode mode, Matrix4f viewMatrix, Matrix4f projectionMatrix, float w, float h) {
        super.setDefaultUniforms(mode, viewMatrix, projectionMatrix, w, h);
        if (this.CANPIPE_RENDER_TARGET != null) {
            Pipeline p = Pipelines.getCurrent();
            int fbID = GlStateManager.getFrameBuffer(GL33C.GL_DRAW_FRAMEBUFFER);
            int renderTarget = -1;
            if (fbID == p.solidFramebuffer.glID() || fbID == p.defaultFramebuffer.glID()) {
                renderTarget = 0;
            }
            else if (fbID == p.translucentTerrainFramebuffer.glID()) {
                renderTarget = 1;
            }
            else if (fbID == p.translucentItemEntityFramebuffer.glID()) {
                renderTarget = 2;
            }
            else if (fbID == p.particlesFramebuffer.glID()) {
                renderTarget = 3;
            }
            this.CANPIPE_RENDER_TARGET.set(renderTarget);
        }
    }*/

    public static MaterialProgram load(
        ResourceLocation pipelineLocation,
        VertexFormat format,
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
        Function<ResourceLocation, Optional<String>> getShaderSource
    ) {
        String vertexSrc = getShaderSource.apply(vertexShaderLocation).get();
        String fragmentSrc = getShaderSource.apply(fragmentShaderLocation).get();

        Function<String, String> commentFrxUniforms = (String src) -> {
            src = src.replace("uniform int frxu_cascade;", "// uniform int frxu_cascade;");
            return src;
        };

        vertexSrc = commentFrxUniforms.apply(vertexSrc);
        fragmentSrc = commentFrxUniforms.apply(fragmentSrc);

        if (shadowFramebuffer != null && shadowFramebuffer.depthAttachment != null) {
            var depthArray = shadowFramebuffer.depthAttachment.texture();
            samplers = Streams.concat(
                samplers.stream(),
                List.of("frxs_shadowMap", "frxs_shadowMapTexture").stream()
            ).toList();
            textureViews = Streams.concat(
                textureViews.stream(),
                List.of(Optional.of(depthArray.view), Optional.of(depthArray.view)).stream()
            ).toList();
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

        String uniformBlock =
            "layout(std140) uniform canpipe_ub_material_program {\n"+
            "   uniform int frxu_cascade;\n"+
            "   uniform int canpipe_renderTarget;\n"+
            "   uniform float canpipe_alphaCutout;\n"+
            "   uniform vec3 canpipe_light0Direction;\n"+
            "   uniform vec3 canpipe_light1Direction;\n"+
            "};\n\n";

        vertexSrc =
            "#define CANPIPE_MATERIAL_SHADER\n"+
            (depthPass ? "#define DEPTH_PASS\n" : "")+
            "\n"+
            uniformBlock+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.POSITION)+") in vec3 in_vertex;  // Position\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.COLOR)+") in vec4 in_color;  // Color\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV0)+") in vec2 in_uv;  // UV0\n"+
            (
                format.contains(VertexFormatElement.UV1) ?
                "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV1)+") in ivec2 in_uv1" :
                "const ivec2 in_v1 = ivec2(0)"
            ) + ";\n"+
            "layout(location = "+format.getElements().indexOf(VertexFormatElement.UV2)+") in ivec2 in_lightmap;  // UV2\n"+
            (
                format.contains(VertexFormatElement.NORMAL) ?
                "layout(location = "+format.getElements().indexOf(VertexFormatElement.NORMAL)+") in vec3 in_normal" :
                "const vec3 in_normal = vec3(0.0, 1.0, 0.0)"
            ) + ";  // Normal\n"+
            "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_FLAGS)+") in int in_materialFlags;\n"+
            (
                format.contains(CanPipe.VertexFormatElements.AO) ?
                "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.AO)+") in float in_ao" :
                "const float in_ao = 1.0"
            ) + ";\n"+
            (
                format.contains(CanPipe.VertexFormatElements.SPRITE_INDEX) ?
                "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.SPRITE_INDEX)+") in int in_spriteIndex" :
                "const int in_spriteIndex = -1"
            ) + ";\n"+
            (
                format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX) ?
                "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.MATERIAL_INDEX)+") in int in_materialIndex" :
                "const int in_materialIndex = -1"
            ) + ";\n"+
            (
                format.contains(CanPipe.VertexFormatElements.TANGENT) ?
                "layout(location = "+format.getElements().indexOf(CanPipe.VertexFormatElements.TANGENT)+") in vec4 in_tangent"
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
            uniformBlock+
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

                if (frx_fragColor.a < canpipe_alphaCutout) {
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

        var vertexShader = Shader.load(
            vertexShaderLocation, vertexSrc, ShaderType.VERTEX, glslVersion,
            options, appliedOptions, getShaderSource, shadowFramebuffer, (s) -> s
        );
        var fragmentShader = Shader.load(
            fragmentShaderLocation, fragmentSrc, ShaderType.FRAGMENT, glslVersion,
            options, appliedOptions, getShaderSource, shadowFramebuffer, (s) -> s
        );

        return new MaterialProgram(
            pipelineLocation, format,
            vertexShader, fragmentShader,
            samplers, textureViews, depthPass
        );
    }

}
