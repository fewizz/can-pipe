package fewizz.canpipe.mixin;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.renderpearl.CommandEncoderExtended;
import fewizz.canpipe.renderpearl.GpuTextureViewExtended;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;


@Mixin(value = LevelRenderer.class, priority = 1001)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow @Final private LevelTargetBundle targets;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow private boolean usingMultiDrawIndirectForTerrain;
    @Shadow private @Nullable GpuSampler chunkLayerSampler;

    @Shadow private void checkPoseStack(PoseStack poseStack) {}
    @Shadow private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
    @Shadow private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
    @Shadow private void submitTransientBlocks(final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final LevelRenderState levelRenderState) {}
    @Shadow private void executeClassicTransparency(ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass) {}
    @Shadow public ChunkSectionsToRender prepareChunkRendersIndirect(final Matrix4fc modelViewMatrix, final boolean respectTranslucentOrder) { return null; }
    @Shadow public ChunkSectionsToRender prepareChunkRenders(final Matrix4fc modelViewMatrix, final boolean respectTranslucentOrder) { return null; }

    @Unique private int canpipe_currentShadowCascadeIdx = -1;
    @Unique private float canpipe_eyeBlockLight = 0.0F;
    @Unique private float canpipe_eyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedEyeBlockLight = 0.0F;
    @Unique private float canpipe_smoothedEyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedRainGradient = 0.0F;
    @Unique private float canpipe_smoothedThunderGradient = 0.0F;

    @SuppressWarnings("unchecked")
    @Unique final private ObjectArrayList<SectionRenderDispatcher.RenderSection>[] canpipe_visibleSections =
        Stream.generate(() -> new ObjectArrayList<SectionRenderDispatcher.RenderSection>(10000))
        .limit(4).toArray(ObjectArrayList[]::new);

    @Unique private ObjectArrayList<SectionRenderDispatcher.RenderSection> canpipe_nearbyVisibleSectionsSink = new ObjectArrayList<>(50);

    @Override public int canpipe_getCurrentShadowCascadeIdx() { return this.canpipe_currentShadowCascadeIdx; }
    @Override public void canpipe_setCurrentShadowCascadeIdx(int cascade) { this.canpipe_currentShadowCascadeIdx = cascade; }
    @Override public float canpipe_getEyeBlockLight() { return this.canpipe_eyeBlockLight; }
    @Override public float canpipe_getEyeSkyLight() { return this.canpipe_eyeSkyLight; }
    @Override public float canpipe_getSmoothedEyeBlockLight() { return this.canpipe_smoothedEyeBlockLight; }
    @Override public float canpipe_getSmoothedEyeSkyLight() { return this.canpipe_smoothedEyeSkyLight; }
    @Override public float canpipe_getSmoothedRainGradient() { return this.canpipe_smoothedRainGradient; }
    @Override public float canpipe_getSmoothedThunderGradient() { return this.canpipe_smoothedThunderGradient; }

    @Inject(method = "render", at = @At(value = "HEAD"))
    void renderShadows(
        GraphicsResourceAllocator resourceAllocator,
        boolean renderOutline,
        CameraRenderState cameraState,
        GpuBufferSlice terrainFog,
        Vector4f fogColor,
        boolean shouldRenderSky,
        boolean consistentDepthRequired,
        CallbackInfo ci
    ) throws Exception {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        final Minecraft mc = Minecraft.getInstance();
        final ProfilerFiller profiler = Profiler.get();

        var eyePosBlockPos = BlockPos.containing(mc.player.getEyePosition());
        int blockLight = mc.level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(eyePosBlockPos);
        int skyLight = mc.level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(eyePosBlockPos);
        skyLight = Math.max(0, skyLight - mc.level.getSkyDarken());

        this.canpipe_eyeBlockLight = blockLight / 15.0F;
        this.canpipe_eyeSkyLight = skyLight / 15.0F;

        float brightnessDelta = 1.0F - (float) Math.pow(Math.E, -1.0 / p.brightnessSmoothingFrames);
        float rainDelta = 1.0F - (float) Math.pow(Math.E, -1.0 / p.rainSmoothingFrames);
        float thunderDelta = 1.0F - (float) Math.pow(Math.E, -1.0 / p.thunderSmoothingFrames);

        this.canpipe_smoothedEyeBlockLight =
            this.canpipe_eyeBlockLight > this.canpipe_smoothedEyeBlockLight && !p.smoothBrightnessBidirectionally
            ? this.canpipe_eyeBlockLight
            : Mth.lerp(brightnessDelta, this.canpipe_smoothedEyeBlockLight, this.canpipe_eyeBlockLight);

        this.canpipe_smoothedEyeSkyLight =
            this.canpipe_eyeSkyLight > this.canpipe_smoothedEyeSkyLight && !p.smoothBrightnessBidirectionally
            ? this.canpipe_eyeSkyLight
            : Mth.lerp(brightnessDelta, this.canpipe_smoothedEyeSkyLight, this.canpipe_eyeSkyLight);

        this.canpipe_smoothedRainGradient = Mth.lerp(rainDelta, this.canpipe_smoothedRainGradient, mc.level.getRainLevel(this.levelRenderState.worldPartialTicks));
        this.canpipe_smoothedThunderGradient = Mth.lerp(thunderDelta, this.canpipe_smoothedThunderGradient, mc.level.getThunderLevel(this.levelRenderState.worldPartialTicks));

        if (p.shadows == null || !mc.level.dimensionType().hasSkyLight()) {
            return;
        }

        profiler.popPush("can-pipe shadows");
        profiler.push("preparations");

        GameRendererExtended gre = ((GameRendererExtended) mc.gameRenderer);

        Matrix4f viewMatrix = gre.canpipe_worldViewMatrix();

        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.mul(viewMatrix);

        boolean entityShadowsOptionValue = mc.options.entityShadows().get();
        mc.options.entityShadows().set(false);

        PoseStack poseStack = new PoseStack();

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        LevelRenderStateExtended lrse = ((LevelRenderStateExtended) this.levelRenderState);

        @SuppressWarnings("deprecation")
        GpuTextureView blockAtlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();

        for (this.canpipe_currentShadowCascadeIdx = 0; this.canpipe_currentShadowCascadeIdx < p.shadows.cascadeRadii().size()+1; ++this.canpipe_currentShadowCascadeIdx) {
            Framebuffer fb = p.shadows.framebuffers().get(this.canpipe_currentShadowCascadeIdx);

            // Same cascade radius? Then just copy
            {
                float cascadeRadius = gre.canpipe_getShadowCenters()[this.canpipe_currentShadowCascadeIdx].w;
                float prevCascadeRadius = this.canpipe_currentShadowCascadeIdx > 0 ? gre.canpipe_getShadowCenters()[this.canpipe_currentShadowCascadeIdx - 1].w : -1.0F;

                if (cascadeRadius == prevCascadeRadius) {
                    Framebuffer prevFb = p.shadows.framebuffers().get(this.canpipe_currentShadowCascadeIdx - 1);
                    ((CommandEncoderExtended) commandEncoder).canpipe_copyTextureToTexture(
                        prevFb.getDepthTexture(), fb.getDepthTexture(),
                        0, 0, 0, 0, 0, fb.width, fb.height,
                        ((GpuTextureViewExtended) prevFb.getDepthTextureView()).canpipe_baseArrayLayer(),
                        ((GpuTextureViewExtended) fb.getDepthTextureView()).canpipe_baseArrayLayer()
                    );
                    continue;
                }
            }

            profiler.popPush("cascade " + this.canpipe_currentShadowCascadeIdx);

            profiler.push("prepare chunk sections to render");

            ChunkSectionsToRender chunkSectionsToRender;
            if (this.usingMultiDrawIndirectForTerrain) {
                chunkSectionsToRender = this.prepareChunkRendersIndirect(viewMatrix, false);
            } else {
                chunkSectionsToRender = this.prepareChunkRenders(viewMatrix, false);
            }
            profiler.push("render sections");

            SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();

            if (p.shadows.allowEntities()) {
                this.submitEntities(poseStack, levelRenderState, submitNodeStorage);
                lrse.canpipe_getEntityRenderStates()[this.canpipe_currentShadowCascadeIdx].clear();

                this.submitBlockEntities(poseStack, levelRenderState, submitNodeStorage);
                lrse.canpipe_getBlockEntityRenderStates()[this.canpipe_currentShadowCascadeIdx].clear();

                this.submitTransientBlocks(poseStack, submitNodeStorage, levelRenderState);
                profiler.popPush("entities");

                profiler.push("submit entities");
                this.submitEntities(poseStack, this.levelRenderState, submitNodeStorage);

                profiler.popPush("submit block entities");
                this.submitBlockEntities(poseStack, this.levelRenderState, submitNodeStorage);

                this.checkPoseStack(poseStack);

                profiler.pop();
            }

            if (p.shadows.allowParticles()) {
                profiler.popPush("particles");

                profiler.push("submit particles");

                ParticlesRenderState state = lrse.canpipe_getParticlesRenderStates()[this.canpipe_currentShadowCascadeIdx];
                state.submit(submitNodeStorage, levelRenderState.cameraRenderState);
                state.particles.clear();

                profiler.pop();
            }

            FeatureRenderDispatcher.PreparedFrame featureFrame = this.featureRenderDispatcher.prepareFrame(submitNodeStorage);

            var descriptorBuilder = RenderPassDescriptor.builder(() -> "can-pipe Shadow Cascade #"+this.canpipe_currentShadowCascadeIdx);
            descriptorBuilder.withDepthAttachment(fb.getDepthTextureView(), OptionalDouble.of(1.0));
            descriptorBuilder.withRenderArea(new RenderPass.RenderArea(0, 0, fb.width, fb.height));

            try (RenderPass renderPass = commandEncoder.createRenderPass(descriptorBuilder.build())) {
                RenderSystem.bindDefaultUniforms(renderPass);
                chunkSectionsToRender.renderGroup(
                    ChunkSectionLayerGroup.OPAQUE,
                    renderPass,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                    blockAtlas,
                    false
                );

                profiler.popPush("render features");

                featureFrame.executeSolid(renderPass);
                featureFrame.executeTranslucent(renderPass);
                featureFrame.executeTranslucentAfterTerrain(renderPass);

                profiler.pop();
            }

            featureFrame.close();
        }

        this.canpipe_currentShadowCascadeIdx = -1;

        modelViewMatrixStack.popMatrix();

        mc.options.entityShadows().set(entityShadowsOptionValue);

        profiler.pop();
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass"
        )
    )
    RenderPass onCreateSolidTerrainRenderPass(
        CommandEncoder commandEncoder,
        Supplier<String> label,
        GpuTextureView colorTexture,
        Optional<Vector4fc> clearColor,
        @Nullable GpuTextureView depthTexture,
        OptionalDouble clearDepth,
        Operation<RenderPass> operation
    ) {
        Pipeline p = Pipelines.getCurrent();

        if (p != null) {
            final Framebuffer fb = p.shadowFramebufferOr(p.solidFramebuffer);

            RenderPassDescriptor.Builder descriptor = RenderPassDescriptor.builder(label);
            for (var colorAttachment : fb.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
            descriptor.withDepthAttachment(fb.getDepthTextureView());
            descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, fb.width, fb.height));

            return commandEncoder.createRenderPass(descriptor.build());
        }

        return operation.call(commandEncoder, label, colorTexture, clearColor, depthTexture, clearDepth);
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;executeClassicTransparency("+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"+
                "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"+
                "Lcom/mojang/renderpearl/api/commands/RenderPass;"+
            ")V"
        )
    )
    void deferTranslucentTerrainRendering(
        LevelRenderer instance, ChunkSectionsToRender chunkSectionsToRender, FeatureRenderDispatcher.PreparedFrame featureFrame, RenderPass renderPass,
        Operation<Void> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) return;

        operation.call(instance, chunkSectionsToRender, featureFrame, renderPass);
    }

    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;executeOutline("+
                "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;"+
            ")V"
        )
    )
    void renderTranslucentTerrain(
        CallbackInfo ci,
        @Local ChunkSectionsToRender chunkSectionsToRender,
        @Local FeatureRenderDispatcher.PreparedFrame featureFrame
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;

        Minecraft mc = Minecraft.getInstance();
        var renderArea = new RenderPass.RenderArea(0, 0, p.defaultFramebuffer.width, p.defaultFramebuffer.height);

        var commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        ((CommandEncoderExtended) commandEncoder).canpipe_copyTextureToTexture(
            p.solidFramebuffer.getDepthTexture(), p.translucentTerrainFramebuffer.getDepthTexture(),
            0, 0, 0, 0, 0, renderArea.width(), renderArea.height(),
            ((GpuTextureViewExtended) p.solidFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer(),
            ((GpuTextureViewExtended) p.translucentTerrainFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer()
        );
        ((CommandEncoderExtended) commandEncoder).canpipe_copyTextureToTexture(
            p.solidFramebuffer.getDepthTexture(), p.translucentItemEntityFramebuffer.getDepthTexture(),
            0, 0, 0, 0, 0, renderArea.width(), renderArea.height(),
            ((GpuTextureViewExtended) p.solidFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer(),
            ((GpuTextureViewExtended) p.translucentItemEntityFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer()
        );
        ((CommandEncoderExtended) commandEncoder).canpipe_copyTextureToTexture(
            p.solidFramebuffer.getDepthTexture(), p.translucentParticlesFramebuffer.getDepthTexture(),
            0, 0, 0, 0, 0, renderArea.width(), renderArea.height(),
            ((GpuTextureViewExtended) p.solidFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer(),
            ((GpuTextureViewExtended) p.translucentParticlesFramebuffer.getDepthTextureView()).canpipe_baseArrayLayer()
        );

        this.executeClassicTransparency(chunkSectionsToRender, featureFrame, null /* no renderpass, see mixins below */);
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucent(Lcom/mojang/renderpearl/api/commands/RenderPass;)V")
    )
    void createRenderPassForTranslucentFeatures(
        FeatureRenderDispatcher.PreparedFrame instance, RenderPass renderPass, Operation<Void> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "Translucent Features");
        for (var colorAttachment : p.solidFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.solidFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.solidFramebuffer.width, p.solidFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, newRenderPass);
        }
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup("+
            "Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;"+
            "Lcom/mojang/renderpearl/api/commands/RenderPass;"+
            "Lcom/mojang/renderpearl/api/textures/GpuSampler;"+
            "Lcom/mojang/renderpearl/api/textures/GpuTextureView;"+
            "Z"+
        ")V")
    )
    void createRenderPassForTranslucentTerrain(
        ChunkSectionsToRender instance, ChunkSectionLayerGroup group, RenderPass renderPass, GpuSampler sampler, GpuTextureView blockAtlas, boolean renderWireframeTerrain,
        Operation<Void> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "Translucent Terrain");
        for (var colorAttachment : p.translucentTerrainFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.translucentTerrainFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.translucentTerrainFramebuffer.width, p.translucentTerrainFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, group, newRenderPass, sampler, blockAtlas, renderWireframeTerrain);
        }
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;executeTranslucentAfterTerrain(Lcom/mojang/renderpearl/api/commands/RenderPass;)V")
    )
    void createRenderPassForAfterTerrainTranslucent(FeatureRenderDispatcher.PreparedFrame instance, RenderPass renderPass, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "Translucent Particles");
        for (var colorAttachment : p.translucentParticlesFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.translucentParticlesFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.translucentParticlesFramebuffer.width, p.translucentParticlesFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, newRenderPass);
        }
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(Lnet/minecraft/client/CloudStatus;Lcom/mojang/renderpearl/api/commands/RenderPass;)V")
    )
    void createRenderPassForClouds(CloudRenderer instance, CloudStatus cloudStatus, RenderPass renderPass, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "Translucent Particles");
        for (var colorAttachment : p.cloudsFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.cloudsFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.cloudsFramebuffer.width, p.cloudsFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, cloudStatus, newRenderPass);
        }
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;render("+
            "Lnet/minecraft/client/renderer/state/level/WeatherRenderState;"+
            "Lcom/mojang/renderpearl/api/commands/RenderPass;"+
        ")V")
    )
    void createRenderPassForWeather(WeatherEffectRenderer instance, WeatherRenderState state, RenderPass renderPass, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "Weather");
        for (var colorAttachment : p.weatherFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.weatherFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.weatherFramebuffer.width, p.weatherFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, state, newRenderPass);
        }
    }

    @WrapOperation(
        method = "executeClassicTransparency",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;render("+
            "Lnet/minecraft/client/renderer/state/level/WorldBorderRenderState;"+
            "Lcom/mojang/renderpearl/api/commands/RenderPass;"+
            "Lnet/minecraft/world/phys/Vec3;"+
            "D"+
        ")V")
    )
    void createRenderPassForWorldBorder(
        WorldBorderRenderer instance, WorldBorderRenderState state, RenderPass renderPass, Vec3 cameraPos, double renderDistance, Operation<Void> operation
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) return;
        if (renderPass != null) throw new RuntimeException("Expected renderPass to be null");

        var descriptor = RenderPassDescriptor.builder(() -> "World Border");
        for (var colorAttachment : p.weatherFramebuffer.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        descriptor.withDepthAttachment(p.weatherFramebuffer.getDepthTextureView());
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, p.weatherFramebuffer.width, p.weatherFramebuffer.height));

        try (RenderPass newRenderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            operation.call(instance, state, newRenderPass, cameraPos, renderDistance);
        }
    }

    @ModifyExpressionValue(
        method = "extractSectionDrawGroups",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/renderer/chunk/ChunkSectionLayer.pipeline(Z)Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;"
        )
    )
    RenderPipeline changeVertexSizeOnExtractSectionDrawGroups(RenderPipeline pipeline) {
        var p = Pipelines.getCurrent();
        if (p != null) {
            MaterialProgramLoader programLoader = p.getMaterialProgramLoader(pipeline);
            pipeline = programLoader.createRenderPipeline(Pair.of(List.of(), GpuFormat.D32_FLOAT));
        }
        return pipeline;
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/api/device/GpuDevice;createSampler("+
                "Lcom/mojang/renderpearl/api/textures/AddressMode;"+
                "Lcom/mojang/renderpearl/api/textures/AddressMode;"+
                "Lcom/mojang/renderpearl/api/textures/FilterMode;"+
                "Lcom/mojang/renderpearl/api/textures/FilterMode;"+
                "I"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/renderpearl/api/textures/GpuSampler;"
        )
    )
    GpuSampler onCreateSampler(GpuDevice device, AddressMode u, AddressMode v, FilterMode min, FilterMode mag, int maxAnisotropy, OptionalDouble maxLod, Operation<GpuSampler> operation) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            min = FilterMode.NEAREST;
            mag = FilterMode.NEAREST;
            maxAnisotropy = 1;
            maxLod = OptionalDouble.empty();
        }
        return operation.call(device, u, v, min, mag, maxAnisotropy, maxLod);
    }

    @ModifyExpressionValue(
        method = {"visibleSections", "clearVisibleSections", "extractSectionDrawGroups", "scheduleTranslucentSectionResort"},
        require = 1+1+1+5,
        at = @At(
            value= "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
        )
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (this.canpipe_currentShadowCascadeIdx >= 0) {
            return this.canpipe_visibleSections[this.canpipe_currentShadowCascadeIdx];
        }
        return original;
    }

   @ModifyExpressionValue(
        method = {"nearbyVisibleSections", "clearVisibleSections", "scheduleTranslucentSectionResort"},
        require = 1+1+1,
        at = @At(
            value= "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;nearbyVisibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
        )
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (this.canpipe_currentShadowCascadeIdx >= 0) {
            return this.canpipe_nearbyVisibleSectionsSink;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = "submitEntities",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;entityRenderStates:Ljava/util/List;"
        )
    )
    List<EntityRenderState> replaceEntityRenderStates(List<EntityRenderState> entityRenderStates) {
        int cascade = this.canpipe_currentShadowCascadeIdx;
        if (cascade >= 0) {
            return ((LevelRenderStateExtended) this.levelRenderState).canpipe_getEntityRenderStates()[cascade];
        }
        return entityRenderStates;
    }

}
