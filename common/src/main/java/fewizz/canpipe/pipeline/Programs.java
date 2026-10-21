package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.renderpearl.RenderPipelineBuilderExtended;
import fewizz.canpipe.util.JanksonUtils;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;


public class Programs {

    static final Predicate<String> CONTAINS_VERTEX_IN = Pattern.compile("\\s*in\\s+vec(3|4)\\s+in_vertex").asPredicate();
    static final Predicate<String> CONTAINS_UV_IN = Pattern.compile("\\s*in\\s+vec2\\s+in_uv").asPredicate();

    private Programs() {}

    static Pending load(
        JsonObject json,
        Identifier pipelineLocation,
        Function<Identifier, Optional<String>> getShaderSource,
        int glslVersion,
        Map<Identifier, OptionGroup> options,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Optional<Integer> shadowMapSize,
        Framebuffer framebuffer,
        boolean awareOfDepthRangeChanges
    ) {
        List<String> samplers = JanksonUtils.listOfStrings(json, "samplers");

        var name = json.get(String.class, "name");
        var vertexLocation = Identifier.parse(json.get(String.class, "vertexSource"));
        var fragmentLocation = Identifier.parse(json.get(String.class, "fragmentSource"));

        var formats = framebuffer.getFormats();
        String postfix = "";
        for (var colorAttachmentFormat : formats.getLeft()) {
            postfix += "-c-"+colorAttachmentFormat.toString();
        }
        if (formats.getRight() != null) {
            postfix += "-d-"+formats.getRight().toString();
        }
        postfix = postfix.replace("_", "-").toLowerCase();

        var renderPipelineBuilder = RenderPipeline.builder()
            .withLocation(pipelineLocation.withSuffix("-"+name).withSuffix(postfix))
            .withVertexShader(vertexLocation)
            .withFragmentShader(fragmentLocation)
            .withCull(false)
            .withVertexBinding(0, CanPipe.VertexFormats.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.QUADS);

        if (!awareOfDepthRangeChanges && RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()) {
            renderPipelineBuilder.withShaderDefine("CANPIPE_CLIP_DEPTH_RANGE_PATCH");
        }

        ((RenderPipelineBuilderExtended) renderPipelineBuilder).canpipe_allowNoColorTargets();

        int colorAttachmentIndex = 0;
        for (var colorAttachmentFormat : formats.getLeft()) {
            renderPipelineBuilder.withColorTargetState(colorAttachmentIndex, new ColorTargetState(
                Optional.empty(),  // no blend function
                colorAttachmentFormat,
                ColorTargetState.WRITE_ALL
            ));
            colorAttachmentIndex += 1;
        }

        var bindGroupLayoutBuilder = BindGroupLayout.builder()
            .withUniform("canpipe_ub_pass", UniformType.UNIFORM_BUFFER)

            .withUniform("frx_ub_accessibility", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_view", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_shadow", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_player", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_world", UniformType.UNIFORM_BUFFER)
            .withUniform("frx_ub_fog", UniformType.UNIFORM_BUFFER);

        for (var externalUboName : Uniforms.EXTERNAL_UBOS.keySet()) {
            bindGroupLayoutBuilder.withUniform(externalUboName, UniformType.UNIFORM_BUFFER);
        }

        for (String sampler : samplers) {
            bindGroupLayoutBuilder.withUniform(sampler, UniformType.COMBINED_IMAGE_SAMPLER);
        }

        renderPipelineBuilder.withBindGroupLayout(bindGroupLayoutBuilder.build());

        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.PROJECTION);
        renderPipelineBuilder.withBindGroupLayout(BindGroupLayouts.FOG);

        RenderPipeline pipeline = renderPipelineBuilder.build();

        Function<String, String> postprocess = (src) -> {
            src = src.replaceAll("uniform\\s+ivec2\\s+frxu_size;", "// uniform ivec2 frxu_size;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_lod;", "// uniform int frxu_lod;");
            src = src.replaceAll("uniform\\s+int\\s+frxu_layer;", "// uniform int frxu_layer;");
            src = src.replaceAll("uniform\\s+mat4\\s+frxu_frameProjectionMatrix;", "// uniform mat4 frxu_frameProjectionMatrix;");
            src =
                "layout(std140) uniform canpipe_ub_pass {\n"+
                "    ivec2 frxu_size;\n"+
                "    int frxu_lod;\n"+
                "    int frxu_layer;\n"+
                "    mat4 frxu_frameProjectionMatrix;\n"+
                "};\n\n"+
                src;
            return src;
        };

        String vertexSrc0 = getShaderSource.apply(vertexLocation).orElse(null);
        String vertexSrc = Shaders.process(
            vertexLocation, vertexSrc0, ShaderType.VERTEX, glslVersion, options, appliedOptions,
            getShaderSource, shadowMapSize, (src) -> {
                // some shaderpacks define them, some - not
                if (!CONTAINS_VERTEX_IN.test(src)) {
                    src = "in vec3 in_vertex;\n\n"+src;
                }
                if (!CONTAINS_UV_IN.test(src)) {
                    src = "in vec2 in_uv;\n\n"+src;
                }

                src =
                    "#define in_vertex Position\n"+
                    "#define in_uv UV0\n\n"+
                    src;

                return postprocess.apply(src);
            }
        );

        String fragmentSrc0 = getShaderSource.apply(fragmentLocation).orElse(null);
        String fragmentSrc = Shaders.process(
            fragmentLocation, fragmentSrc0, ShaderType.FRAGMENT, glslVersion, options, appliedOptions,
            getShaderSource, shadowMapSize, postprocess
        );

        var futurePending = RenderSystem.getDevice().compilePipeline(pipeline, new ShaderSource() {

            @Override
            public @Nullable String getShader(Identifier id, ShaderType type) {
                if (type == ShaderType.VERTEX) {
                    return vertexSrc;
                }
                if (type == ShaderType.FRAGMENT) {
                    return fragmentSrc;
                }
                throw new RuntimeException();
            }

            @Override
            public @Nullable CachedIncludeSource getInclude(Identifier id) { return null; }

            @Override
            public void close() {}

        }, Util.backgroundExecutor());

        return new Pending(futurePending, pipeline);
    }

    static class Pending implements CompiledRenderPipeline.Pending, AutoCloseable {

        // private final CompiledRenderPipeline.Pending pending;
        public final CompletableFuture<CompiledRenderPipeline.Pending> future;
        public final RenderPipeline renderPipeline;
        private CompiledRenderPipeline compiledRenderPipeline = null;

        public Pending(CompletableFuture<CompiledRenderPipeline.Pending> futurePending, RenderPipeline renderPipeline) {
            this.future = futurePending;
            this.renderPipeline = renderPipeline;
        }

        @Override
        public @Nullable CompiledRenderPipeline finishCompile() {
            if (this.compiledRenderPipeline != null) {
                throw new RuntimeException("Pipeline is already finished compiling");
            }
            this.compiledRenderPipeline = this.future.resultNow().finishCompile();
            return this.compiledRenderPipeline;
        }

        public CompiledRenderPipeline compiledRenderPipeline() {
            if (this.compiledRenderPipeline == null) {
                throw new RuntimeException("Pipeline isn't finished compiling");
            }
            return this.compiledRenderPipeline;
        }

        @Override
        public void close() throws Exception {
            if (!this.future.isDone()) {
                throw new RuntimeException("Future isn't completed");
            }
            if (this.compiledRenderPipeline != null) {
                this.compiledRenderPipeline.close();
            }
        }

    }

}
