package fewizz.canpipe.pipeline;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.lwjgl.util.spvc.Spvc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.renderpearl.PipelineBuilderExtended;
import fewizz.canpipe.renderpearl.RenderPipelineBuilderExtended;
import fewizz.canpipe.renderpearl.mixin.FrontendGpuDeviceAccessor;
import fewizz.canpipe.util.JanksonUtils;
import fewizz.canpipe.util.SimpleShaderSource;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;


public class Programs {

    static final Pattern IN_VERTEX_VEC4_PATTERN = Pattern.compile("in\\s+vec4\\s+in_vertex\\s*;");
    static final Pattern IN_VERTEX_VEC3_PATTERN = Pattern.compile("in\\s+vec3\\s+in_vertex\\s*;");
    static final Pattern IN_UV_PATTERN = Pattern.compile("in\\s+vec2\\s+in_uv\\s*;");

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
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
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

        String vertexSrc = getShaderSource.apply(vertexLocation).orElse(null);
        String vertexSrcProcessed = Shaders.process(
            vertexLocation, vertexSrc, ShaderType.VERTEX, glslVersion, options, appliedOptions,
            getShaderSource, shadowMapSize, (src) -> {
                src =
                    "in vec3 Position;\n"+
                    "in vec2 UV0;\n\n"+
                    src;

                var inPositionVec4Matcher = IN_VERTEX_VEC4_PATTERN.matcher(src);
                var inPositionVec3Matcher = IN_VERTEX_VEC3_PATTERN.matcher(src);
                if (inPositionVec4Matcher.find()) {
                    src = inPositionVec4Matcher.replaceFirst("#define in_vertex (vec4(Position, 1.0))");
                }
                else if (inPositionVec3Matcher.find()) {
                    src = inPositionVec3Matcher.replaceFirst("#define in_vertex Position");
                }
                else {
                    src = "#define in_vertex Position\n\n" + src;
                }

                var uvMatcher = IN_UV_PATTERN.matcher(src);
                if (uvMatcher.find()) {
                    src = uvMatcher.replaceFirst("#define in_uv UV0");
                }
                else {
                    src = "#define in_uv UV0\n\n" + src;
                }

                return postprocess.apply(src);
            }
        );

        String fragmentSrc = getShaderSource.apply(fragmentLocation).orElse(null);
        String fragmentSrcProcessed = Shaders.process(
            fragmentLocation, fragmentSrc, ShaderType.FRAGMENT, glslVersion, options, appliedOptions,
            getShaderSource, shadowMapSize, postprocess
        );

        var shaderSource = new SimpleShaderSource(vertexLocation, vertexSrcProcessed, fragmentLocation, fragmentSrcProcessed);

        var pipelineBuilder = ((FrontendGpuDeviceAccessor) RenderSystem.getDevice()).canpipe_getPipelineBuilder();
        var futurePending = ((PipelineBuilderExtended) pipelineBuilder).canpipe_compilePipeline(
            pipeline, shaderSource,
            Util.backgroundExecutor(), // !!!
            (descriptor) -> {
                var type = descriptor.resourceType() == Spvc.SPVC_RESOURCE_TYPE_SAMPLED_IMAGE ? "Sampler" : "Uniform";
                CanPipe.LOGGER.warn(type+" \""+descriptor.name()+"\" isn't handled by program \""+name+"\"");
            },
            true  // autoMapLocations
        );

        var pending = new Pending(pipeline);
        futurePending.handle((p, t) -> t != null ? pending.completeExceptionally(t) : pending.complete(p));
        return pending;
    }

    static class Pending extends CompletableFuture<CompiledRenderPipeline.Pending> implements CompiledRenderPipeline.Pending, AutoCloseable {

        public final RenderPipeline renderPipeline;
        private CompiledRenderPipeline compiledRenderPipeline = null;

        public Pending(RenderPipeline renderPipeline) {
            this.renderPipeline = renderPipeline;
        }

        @Override
        public @Nullable CompiledRenderPipeline finishCompile() {
            if (this.compiledRenderPipeline != null) {
                throw new RuntimeException("Pipeline is already finished compiling");
            }
            this.compiledRenderPipeline = this.resultNow().finishCompile();
            if (this.compiledRenderPipeline == null) {
                throw new RuntimeException("Couldn't compile pipeline \""+this.renderPipeline.getLocation()+"\"");
            }
            return this.compiledRenderPipeline;
        }

        public CompiledRenderPipeline compiledRenderPipeline() {
            if (this.compiledRenderPipeline == null) {
                throw new RuntimeException("Pipeline isn't finished compiling");
            }
            return this.compiledRenderPipeline;
        }

        public boolean finishedCompiling() {
            return this.compiledRenderPipeline != null;
        }

        @Override
        public void close() {
            if (!this.isDone()) {
                throw new RuntimeException("Future isn't completed");
            }
            if (this.compiledRenderPipeline != null) {
                this.compiledRenderPipeline.close();
            }
        }

    }

}
