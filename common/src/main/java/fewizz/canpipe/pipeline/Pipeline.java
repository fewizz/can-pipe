package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.Level;


public class Pipeline implements AutoCloseable {

    public static record Targets(
        Framebuffer main,
        Framebuffer solid,
        Framebuffer translucentTerrain,
        Framebuffer translucentItemEntity,
        Framebuffer translucentParticles,
        Framebuffer weather,
        Framebuffer clouds
    ) {}

    public static record FabulousTargets(
        Framebuffer translucentTerrain,
        Framebuffer translucentItemEntity,
        Framebuffer translucentParticles,
        Framebuffer weather,
        Framebuffer clouds
    ) {}

    public static record Shadows(
        Map<RenderPipeline, MaterialProgramProvider> materialProgramsLoaders,
        List<Framebuffer> framebuffers,
        List<Integer> cascadeRadii,  // for cascades 1-3, cascade 0 has max radius (render distance)
        float offsetSlopeFactor,
        float offsetBiasUnits,
        boolean supportForwardRender,  // isn't used in canvas
        boolean allowEntities,
        boolean allowParticles  // isn't used in canvas
    ) {}

    public static record Settings(
        float defaultZenithAngle,
        boolean smoothBrightnessBidirectionally,
        int brightnessSmoothingFrames,  // I wonder why smoothing is frame dependent, not time?
        int rainSmoothingFrames,
        int thunderSmoothingFrames,
        boolean awareOfDepthRangeChanges
    ) {}

    public static record Passes(
        List<Pass> onInit,
        List<Pass> beforeWorldRender,
        List<Pass> fabulous,
        List<Pass> afterRenderHand,
        List<Pass> onResize
    ) implements AutoCloseable {

        @Override
        public void close() {
            this.onInit.forEach(Pass::close);
            this.onResize.forEach(Pass::close);
            this.beforeWorldRender.forEach(Pass::close);
            this.fabulous.forEach(Pass::close);
            this.afterRenderHand.forEach(Pass::close);
        }

    }

    public final Identifier location;
    public final Map<OptionGroup.Element<?>, Object> appliedOptions;

    public final Settings settings;
    public final Targets targets;
    public final @Nullable FabulousTargets fabulousTargets;
    public final @Nullable Shadows shadows;

    private final Map<RenderPipeline, MaterialProgramProvider> materialProgramProviders;
    private final Map<String, ? extends AbstractTexture> materialProgramSamplerTextures;

    private final Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, CompiledRenderPipeline> programs;
    private final Map<String, Texture> textures;
    private final Map<String, Framebuffer> framebuffers;
    private final Passes passes;

    public final Map<Pair<List<GpuFormat>, RenderPipeline>, CompiledRenderPipeline> replacedRenderPipelines = new HashMap<>();

    public Pipeline(
        Identifier location,
        Map<OptionGroup.Element<?>, Object> appliedOptions,
        Pipeline.Settings settings,
        Pipeline.Passes passes,
        Pipeline.Shadows shadows,
        Pipeline.Targets targets,
        Pipeline.FabulousTargets fabulousTargets,
        Map<String, Texture> textures,
        Map<String, Framebuffer> framebuffers,
        Map<RenderPipeline, MaterialProgramProvider> materialProgramProviders,
        Map<String, AbstractTexture> materialProgramSamplerTextures,
        Map<Pair<String, Pair<List<GpuFormat>, GpuFormat>>, CompiledRenderPipeline> programs
    ) {
        this.location = location;
        this.appliedOptions = appliedOptions;
        this.settings = settings;
        this.passes = passes;
        this.shadows = shadows;
        this.targets = targets;
        this.fabulousTargets = fabulousTargets;

        this.textures = textures;
        this.framebuffers = framebuffers;
        this.materialProgramProviders = materialProgramProviders;
        this.materialProgramSamplerTextures = materialProgramSamplerTextures;
        this.programs = programs;
    }

    @Override
    public void close() {
        this.passes.close();

        this.framebuffers.values().forEach(Framebuffer::destroyBuffers);
        this.textures.values().forEach(Texture::close);

        this.programs.values().stream().forEach(CompiledRenderPipeline::close);

        this.replacedRenderPipelines.values().forEach(CompiledRenderPipeline::close);
        this.materialProgramProviders.values().forEach(MaterialProgramProvider::close);

        if (this.shadows != null) {
            this.shadows.materialProgramsLoaders.values().forEach(MaterialProgramProvider::close);
        }
    }

    public void onWindowSizeChanged() {
        this.textures.values().forEach(Texture::onWindowSizeChanged);
        this.framebuffers.values().forEach(Framebuffer::onWindowSizeChanged);
    }

    public void onBeforeRenderingLevel(boolean runResizePasses, boolean runInitPasses) {
        Profiler.get().push("can-pipe before world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        if (runInitPasses) {
            for (Pass pass : this.passes.onInit) {
                pass.apply(commandEncoder);
            }
        }

        if (runResizePasses) {
            for (Pass pass : this.passes.onResize) {
                pass.apply(commandEncoder);
            }
        }

        for (Pass pass : this.passes.beforeWorldRender) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterWorldRender() {
        Profiler.get().push("can-pipe after world");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.passes.fabulous) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public void onAfterRenderHand() {
        Profiler.get().push("can-pipe after hand");

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        for (Pass pass : this.passes.afterRenderHand) {
            pass.apply(commandEncoder);
        }

        Profiler.get().pop();
    }

    public MaterialProgramProvider getMaterialProgramLoader(RenderPipeline renderPipeline) {
        Minecraft mc = Minecraft.getInstance();

        if (this.shadows != null && ((LevelRendererExtended) mc.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0) {
            return this.shadows.materialProgramsLoaders().get(renderPipeline);
        }
        else {
            return this.materialProgramProviders.get(renderPipeline);
        }
    }

    public static void bindSpritesExtentsSampler(RenderPass renderPass, @Nullable GpuTextureView sampler0) {
        var mc = Minecraft.getInstance();
        MutableObject<TextureAtlas> atlas = new MutableObject<>();

        if (sampler0 != null) {
            mc.getAtlasManager().forEach((loc, possibleAtlas) -> {
                if (atlas.get() == null && possibleAtlas.getTexture() == sampler0.texture()) {
                    atlas.setValue(possibleAtlas);
                }
            });
        }

        if (atlas.get() == null) {  // we just need to bind something
            atlas.setValue(mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS));
        }

        renderPass.setUniform(
            "canpipe_spritesExtents",
            ((TextureAtlasExtended) atlas.get()).canpipe_getSpritesExtentsBuffer()
        );
    }

    public void bindUniforms(RenderPass renderPass, Framebuffer framebuffer, boolean bindVanillaTextures) {
        GameRendererExtended gre = (GameRendererExtended) Minecraft.getInstance().gameRenderer;
        LevelRendererExtended lre = (LevelRendererExtended) Minecraft.getInstance().levelRenderer;

        Uniforms.setRenderPassFREXUniforms(renderPass);

        renderPass.setUniform("canpipe_ub_origin_type", CanPipe.get0to3UBOBuffers()[gre.canpipe_getOriginType()]);
        renderPass.setUniform("canpipe_ub_is_rendering_hand", CanPipe.get0to3UBOBuffers()[gre.canpipe_isRenderingHand() ? 1 : 0]);
        if (lre.canpipe_getCurrentShadowCascadeIdx() >= 0) {
            renderPass.setUniform("frxu_ub_cascade", CanPipe.get0to3UBOBuffers()[lre.canpipe_getCurrentShadowCascadeIdx()]);
        }

        int renderTarget = 0;
        if (framebuffer == this.targets.translucentTerrain) {
            renderTarget = 1;
        }
        if (framebuffer == this.targets.translucentItemEntity) {
            renderTarget = 2;
        }
        if (framebuffer == this.targets.translucentParticles) {
            renderTarget = 3;
        }

        renderPass.setUniform("canpipe_ub_render_target", CanPipe.get0to3UBOBuffers()[renderTarget]);

        if (bindVanillaTextures) {
            // We don't know yet which render pipeline will be used
            renderPass.setUniform("Sampler0", CanPipe.getWhitePixelTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.setUniform("Sampler1", Minecraft.getInstance().gameRenderer.overlayTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.setUniform("Sampler2", Minecraft.getInstance().gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        }

        for (var e : this.materialProgramSamplerTextures.entrySet()) {
            AbstractTexture texture = e.getValue();
            renderPass.setUniform(e.getKey(), texture.getTextureView(), texture.getSampler());
        }
    }

    public Vector3f getSunOrMoonDir(Level level, Vector3f result) {
        // 0.0 - noon, 0.5 - midnight
        float hourAngle = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.skyRenderState.sunAngle;
        long ticks = level.getDefaultClockTime() % 24000L;

        result.set(
            (float) (-Math.sin(hourAngle)),
            (float) ( Math.cos(hourAngle) *  Math.cos(this.settings.defaultZenithAngle)),
            (float) ( Math.cos(hourAngle) * -Math.sin(this.settings.defaultZenithAngle))
        );

        if (ticks > 13000L && ticks < 23000L) {  // moon
            result.negate();
        }

        return result;
    }

    public Framebuffer shadowFramebufferOr(Framebuffer or) {
        int cascade = ((LevelRendererExtended) Minecraft.getInstance().levelRenderer).canpipe_getCurrentShadowCascadeIdx();
        return cascade >= 0 ? this.shadows.framebuffers.get(cascade) : or;
    }

    public CompiledRenderPipeline onRenderPassSetPipeline(RenderPipeline pipeline, List<RenderPassDescriptor.Attachment<Optional<Vector4fc>>> colorAttachments) {
        if (!pipeline.getLocation().getNamespace().equals("minecraft")) { return null; }

        boolean patchDepth = !this.settings.awareOfDepthRangeChanges;
        boolean patchAttachment = (
            colorAttachments.size() != pipeline.getColorTargetStates().size() ||
            (!colorAttachments.isEmpty() && colorAttachments.getFirst().textureView().texture().getFormat() != GpuFormat.RGBA8_UNORM)
        );

        if (!patchAttachment && !patchDepth) { return null; }

        var colorFormats = colorAttachments.stream().map(a -> a.textureView().texture().getFormat()).toList();

        return this.replacedRenderPipelines.computeIfAbsent(Pair.of(colorFormats, pipeline), _key -> {
            List<ColorTargetState> colorTargets = pipeline.getColorTargetStates();
            if (patchAttachment) {
                colorTargets = new ArrayList<>();
                for (int idx = 0; idx < colorAttachments.size(); ++idx) {
                    var attachment  = colorAttachments.get(idx);
                    var format = attachment.textureView().texture().getFormat();
                    boolean blend = CanPipe.textureFormatSupportsBlend(format);
                    var colorTargetState = pipeline.getColorTargetStates().get(
                        idx < pipeline.getColorTargetStates().size() ? idx : 0  // questionable
                    );
                    colorTargets.add(new ColorTargetState(
                        blend ? colorTargetState.blendFunction() : Optional.empty(),
                        format,
                        colorTargetState.writeMask()
                    ));
                }
            }

            DepthStencilState depthState = pipeline.getDepthStencilState();
            if (patchDepth && depthState != null) {
                depthState = new DepthStencilState(
                    CanPipe.reverseCompareOp(depthState.depthTest()),
                    depthState.writeDepth(),
                    -depthState.depthBiasScaleFactor(),
                    -depthState.depthBiasConstant()
                );
            }

            Minecraft mc = Minecraft.getInstance();
            Map<Pair<Identifier, ShaderType>, String> shaderSources = new HashMap<>();
            Map<Identifier, ShaderSource.CachedIncludeSource> includeSourcesBuilder = new HashMap<>();

            var defines = pipeline.getShaderDefines();
            if (patchDepth) {
                defines = defines.withOverrides(ShaderDefines.builder().define("CANPIPE_CLIP_DEPTH_RANGE_PATCH").build());
            }

            var newRenderPipeline = new RenderPipeline(
                pipeline.getLocation(),
                pipeline.getShaders(),
                defines,
                pipeline.getBindGroupLayouts(),
                colorTargets.toArray(new ColorTargetState[0]),
                depthState,
                pipeline.getPolygonMode(),
                pipeline.isCull(),
                pipeline.getVertexFormatBindings().toArray(new VertexFormat[0]),
                pipeline.getPrimitiveTopology(),
                pipeline.pushConstantSize(),
                pipeline.getSortKey()
            ) {};

            for (Entry<Identifier, Resource> entry : mc.getResourceManager().listResources("shaders", p -> true).entrySet()) {
                Identifier location = entry.getKey();
                ShaderType shaderType = ShaderType.byLocation(location);
                Resource resource = entry.getValue();

                try {
                    if (shaderType != null) {
                        String contents = resource.readAllAsString();
                        Identifier id = shaderType.idConverter().fileToId(location);
                        shaderSources.put(Pair.of(id, shaderType), contents);
                    }
                    else if (ShaderManager.SHADER_INCLUDE_CONVERTER.matches(location)) {
                        String contents = resource.readAllAsString();
                        Identifier id = ShaderManager.SHADER_INCLUDE_CONVERTER.fileToId(location).withSuffix(ShaderManager.SHADER_INCLUDE_EXTENSION);
                        includeSourcesBuilder.put(id, ShaderSource.CachedIncludeSource.create(id, contents));
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            var device = RenderSystem.getDevice();
            return device.compilePipeline(
                newRenderPipeline,
                new ShaderSource() {

                    @Override
                    public @Nullable String getShader(Identifier id, ShaderType type) {
                        return shaderSources.get(Pair.of(id, type));
                    }

                    @Override
                    public @Nullable CachedIncludeSource getInclude(Identifier id) {
                        return includeSourcesBuilder.get(id);
                    }

                    @Override
                    public void close() {
                    }

                },
                Runnable::run  // TODO?
            ).join().finishCompile();
        });
    }

}