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
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
import com.mojang.renderpearl.api.commands.RenderPass.RenderArea;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.MaterialProgramLoader;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.renderpearl.CommandEncoderExtended;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LightLayer;


@Mixin(value = LevelRenderer.class, priority = 1001)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow @Final private LevelTargetBundle targets;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow private boolean usingMultiDrawIndirectForTerrain;

    @Shadow private void checkPoseStack(PoseStack poseStack) {}
    @Shadow private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
    @Shadow private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
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
        float pt = 0.0F; // deltaTracker.getGameTimeDeltaPartialTick(false);  // TODO

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

        this.canpipe_smoothedRainGradient = Mth.lerp(rainDelta, this.canpipe_smoothedRainGradient, mc.level.getRainLevel(pt));
        this.canpipe_smoothedThunderGradient = Mth.lerp(thunderDelta, this.canpipe_smoothedThunderGradient, mc.level.getThunderLevel(pt));

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
        CommandEncoderExtended commandEncoderExtended = (CommandEncoderExtended) RenderSystem.getDevice().createCommandEncoder();
        LevelRenderStateExtended lrse = ((LevelRenderStateExtended) this.levelRenderState);

        /*GpuTexture shadowTexture = p.shadows.framebuffers().get(0).getDepthTexture();
        commandEncoderExtended.canpipe_clearDepthTexture(
            shadowTexture,
            1.0,
            0, shadowTexture.getMipLevels(),
            0, shadowTexture.getDepthOrLayers()
        );*/

        GpuTextureView blockAtlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();

        for (this.canpipe_currentShadowCascadeIdx = 0; this.canpipe_currentShadowCascadeIdx < p.shadows.cascadeRadii().size()+1; ++this.canpipe_currentShadowCascadeIdx) {
            profiler.popPush("cascade " + this.canpipe_currentShadowCascadeIdx);

            profiler.push("prepare chunk sections to render");

            Framebuffer fb = p.shadows.framebuffers().get(this.canpipe_currentShadowCascadeIdx);

            ChunkSectionsToRender chunkSectionsToRender;
            if (this.usingMultiDrawIndirectForTerrain) {
                chunkSectionsToRender = this.prepareChunkRendersIndirect(viewMatrix, !mc.gameRenderer.useImprovedTransparency());
            } else {
                chunkSectionsToRender = this.prepareChunkRenders(viewMatrix, !mc.gameRenderer.useImprovedTransparency());
            }
            profiler.push("render sections");

            var descriptorBuilder = RenderPassDescriptor.builder(() -> "can-pipe Shadow Cascade #"+this.canpipe_currentShadowCascadeIdx);
            descriptorBuilder.withDepthAttachment(fb.getDepthTextureView(), OptionalDouble.of(1.0));
            descriptorBuilder.withRenderArea(new RenderArea(0, 0, fb.width, fb.height));

            try (RenderPass renderPass = commandEncoder.createRenderPass(descriptorBuilder.build())) {
                RenderSystem.bindDefaultUniforms(renderPass);
                chunkSectionsToRender.renderGroup(
                    ChunkSectionLayerGroup.OPAQUE,
                    renderPass,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                    blockAtlas,
                    false
                );
            }

            /*if (p.shadows.allowEntities()) {
                profiler.popPush("entities");

                profiler.push("submit entities");
                this.submitEntities(poseStack, this.levelRenderState, this.submitNodeStorage);

                profiler.popPush("submit block entities");
                this.submitBlockEntities(poseStack, this.levelRenderState, this.submitNodeStorage);

                this.checkPoseStack(poseStack);

                profiler.pop();
            }

            if (p.shadows.allowParticles()) {
                profiler.popPush("particles");

                profiler.push("submit particles");
                ParticlesRenderState state = lrse.canpipe_getParticlesRenderStates()[this.canpipe_currentShadowCascadeIdx];
                state.submit(this.submitNodeStorage, levelRenderState.cameraRenderState);

                state.particles.clear();

                profiler.pop();
            }*/

            profiler.popPush("render features");
            // TODO
            // this.featureRenderDispatcher.renderAllFeatures(this.submitNodeStorage);
            profiler.pop();
        }

        this.canpipe_currentShadowCascadeIdx = -1;

        modelViewMatrixStack.popMatrix();

        mc.options.entityShadows().set(entityShadowsOptionValue);

        profiler.pop();
    }

    // TODO
    /*@WrapOperation(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;getTransparencyChain()Lnet/minecraft/client/renderer/PostChain;"
        )
    )
    PostChain onTransparencyPostChainCreation(
        LevelRenderer instance,
        Operation<PostChain> operation,
        @Local FrameGraphBuilder frame
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return operation.call(instance);  // Initialise transparency post chain normally
        }
        // Don't create transparency post chain, will be handled by pipeline

        // this.targets.main = ...
        // TODO
        // this.targets.translucent = frame.importExternal("translucent", p.fabulousTargets.translucentTerrainFramebuffer());
        // this.targets.itemEntity = frame.importExternal("item_entity", p.fabulousTargets.translucentItemEntityFramebuffer());
        // this.targets.particles = frame.importExternal("particles", p.fabulousTargets.translucentParticlesFramebuffer());
        // this.targets.weather = frame.importExternal("weather", p.fabulousTargets.weatherFramebuffer());
        // this.targets.clouds = frame.importExternal("clouds", p.fabulousTargets.cloudsFramebuffer());

        return null;
    }*/

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
            if (fb.getDepthTextureView() != null) { descriptor.withDepthAttachment(fb.getDepthTextureView()); }
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

        final Framebuffer fb = p.translucentTerrainFramebuffer;

        var commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.copyTextureToTexture(
            p.solidFramebuffer.getDepthTexture(), fb.getDepthTexture(),
            0, 0, 0, 0, 0, fb.width, fb.height
        );

        RenderPassDescriptor.Builder descriptor = RenderPassDescriptor.builder(() -> "Translucent");
        for (var colorAttachment : fb.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
        if (fb.getDepthTextureView() != null) { descriptor.withDepthAttachment(fb.getDepthTextureView()); }
        descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, fb.width, fb.height));

        try (RenderPass newRenderPass = commandEncoder.createRenderPass(descriptor.build())) {
            RenderSystem.bindDefaultUniforms(newRenderPass);
            this.executeClassicTransparency(chunkSectionsToRender, featureFrame, newRenderPass);
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

}
