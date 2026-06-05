package fewizz.canpipe.mixin;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.helpers.NullBufferSource;
import fewizz.canpipe.helpers.PerVertexFormatBufferSource;
import fewizz.canpipe.helpers.ShadowFrustum;
import fewizz.canpipe.mixininterface.FeatureRenderDispatcherExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.LightLayer;


@Mixin(value = LevelRenderer.class, priority = 1001)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow @Final private LevelTargetBundle targets;
    @Shadow @Final private RenderBuffers renderBuffers;

    @Shadow private void checkPoseStack(PoseStack poseStack) {}
    @Shadow private void cullTerrain(Camera camera, Frustum frustum, boolean spectator) {}
    @Shadow private void extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState) {}
    @Shadow private void extractVisibleBlockEntities(Camera camera, float dt, LevelRenderState levelRenderState) {}
    @Shadow private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeStorage submitNodeStorage) {}
    @Shadow private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
    @Shadow private void applyFrustum(Frustum frustum) {}
    @Shadow public ChunkSectionsToRender prepareChunkRenders(final Matrix4fc modelViewMatrix) { return null; }

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

    @Unique private PerVertexFormatBufferSource canpipe_perVertexFormetBufferSource = new PerVertexFormatBufferSource();

    @Override public int canpipe_getCurrentShadowCascadeIdx() { return this.canpipe_currentShadowCascadeIdx; }
    @Override public float canpipe_getEyeBlockLight() { return this.canpipe_eyeBlockLight; }
    @Override public float canpipe_getEyeSkyLight() { return this.canpipe_eyeSkyLight; }
    @Override public float canpipe_getSmoothedEyeBlockLight() { return this.canpipe_smoothedEyeBlockLight; }
    @Override public float canpipe_getSmoothedEyeSkyLight() { return this.canpipe_smoothedEyeSkyLight; }
    @Override public float canpipe_getSmoothedRainGradient() { return this.canpipe_smoothedRainGradient; }
    @Override public float canpipe_getSmoothedThunderGradient() { return this.canpipe_smoothedThunderGradient; }

    @Inject(
        method = "extractLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders("+
                "Lorg/joml/Matrix4fc;"+
            ")Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"
        )
    )
    void extractShadowedEntities(
        CallbackInfo ci,
        @Local Camera camera,
        @Local DeltaTracker deltaTracker,
        @Local ProfilerFiller profiler,
        @Local(ordinal = 0) float dt,
        @Local(ordinal = 0) Matrix4f viewMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null || p.shadows == null) { return; }

        GameRendererExtended gre = ((GameRendererExtended) this.minecraft.gameRenderer);
        LevelRenderStateExtended lrse = ((LevelRenderStateExtended) this.levelRenderState);

        try {
            for (this.canpipe_currentShadowCascadeIdx = 0; this.canpipe_currentShadowCascadeIdx < p.shadows.cascadeRadii().size()+1; ++this.canpipe_currentShadowCascadeIdx) {
                profiler.popPush("can-pipe cascade "+this.canpipe_currentShadowCascadeIdx);

                ShadowFrustum frustum = ((GameRendererExtended) this.minecraft.gameRenderer).canpipe_getShadowFrustums()[this.canpipe_currentShadowCascadeIdx];

                profiler.push("apply frustum");
                applyFrustum(gre.canpipe_getShadowFrustums()[this.canpipe_currentShadowCascadeIdx]);

                profiler.push("prepare chunk sections to render");
                lrse.canpipe_getChunkSectionsToRender()[this.canpipe_currentShadowCascadeIdx] = (prepareChunkRenders(viewMatrix));

                profiler.popPush("shadowed entities");
                this.extractVisibleEntities(camera, frustum, deltaTracker, this.levelRenderState);

                profiler.popPush("shadowed block entities");
                this.extractVisibleBlockEntities(camera, dt, this.levelRenderState);

                profiler.popPush("particles");
                ParticlesRenderState state = lrse.canpipe_getParticlesRenderStates()[this.canpipe_currentShadowCascadeIdx];
                this.minecraft.particleEngine.extract(state, frustum, camera, dt);

                profiler.pop();
            }
        } finally {
            this.canpipe_currentShadowCascadeIdx = -1;
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    void renderShadows(
        final GraphicsResourceAllocator resourceAllocator,
        final DeltaTracker deltaTracker,
        final boolean renderOutline,
        final CameraRenderState cameraState,
        final Matrix4fc modelViewMatrix,
        final GpuBufferSlice terrainFog,
        final Vector4f fogColor,
        final boolean shouldRenderSky,
        final ChunkSectionsToRender chunkSectionsToRender,
        CallbackInfo ci
    ) throws Exception {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        final ProfilerFiller profiler = Profiler.get();
        float pt = deltaTracker.getGameTimeDeltaPartialTick(false);

        var eyePosBlockPos = BlockPos.containing(this.minecraft.player.getEyePosition());
        int blockLight = this.minecraft.level.getLightEngine().getLayerListener(LightLayer.BLOCK).getLightValue(eyePosBlockPos);
        int skyLight = this.minecraft.level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(eyePosBlockPos);
        skyLight = Math.max(0, skyLight - this.minecraft.level.getSkyDarken());

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

        this.canpipe_smoothedRainGradient = Mth.lerp(rainDelta, this.canpipe_smoothedRainGradient, this.minecraft.level.getRainLevel(pt));
        this.canpipe_smoothedThunderGradient = Mth.lerp(thunderDelta, this.canpipe_smoothedThunderGradient, this.minecraft.level.getThunderLevel(pt));

        if (p.shadows == null || !this.minecraft.level.dimensionType().hasSkyLight()) {
            return;
        }

        profiler.popPush("can-pipe shadows");
        profiler.push("preparations");

        GameRendererExtended gre = ((GameRendererExtended) this.minecraft.gameRenderer);

        Matrix4f viewMatrix = gre.canpipe_worldViewMatrix();

        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.mul(viewMatrix);

        boolean entityShadowsOptionValue = this.minecraft.options.entityShadows().get();
        this.minecraft.options.entityShadows().set(false);

        PoseStack poseStack = new PoseStack();

        CommandEncoderExtended commandEncoder = (CommandEncoderExtended) RenderSystem.getDevice().createCommandEncoder();
        LevelRenderStateExtended lrse = ((LevelRenderStateExtended) this.levelRenderState);

        GpuTexture shadowTexture = p.shadows.framebuffers().get(0).getDepthTexture();
        commandEncoder.canpipe_clearDepthTexture(
            shadowTexture,
            1.0,
            0, shadowTexture.getMipLevels(),
            0, shadowTexture.getDepthOrLayers()
        );

        for (this.canpipe_currentShadowCascadeIdx = 0; this.canpipe_currentShadowCascadeIdx < p.shadows.cascadeRadii().size()+1; ++this.canpipe_currentShadowCascadeIdx) {
            profiler.popPush("cascade " + this.canpipe_currentShadowCascadeIdx);

            try {
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setBufferSourceOverride(this.canpipe_perVertexFormetBufferSource);
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setCrumblingBufferSourceOverride(new NullBufferSource());

                profiler.push("render sections");
                ChunkSectionsToRender sections = lrse.canpipe_getChunkSectionsToRender()[this.canpipe_currentShadowCascadeIdx];
                sections.renderGroup(ChunkSectionLayerGroup.OPAQUE, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));

                if (p.shadows.allowEntities()) {
                    profiler.popPush("entities");

                    profiler.push("submit entities");
                    this.submitEntities(poseStack, this.levelRenderState, this.submitNodeStorage);

                    profiler.popPush("submit block entities");
                    this.submitBlockEntities(poseStack, this.levelRenderState, this.submitNodeStorage);

                    profiler.popPush("render features");
                    this.featureRenderDispatcher.renderAllFeatures();
                    this.checkPoseStack(poseStack);

                    profiler.popPush("per-vertex format end batch");
                    this.canpipe_perVertexFormetBufferSource.endBatch();

                    profiler.popPush("end batch");
                    this.renderBuffers.bufferSource().endBatch();

                    profiler.pop();
                }

                if (p.shadows.allowParticles()) {
                    profiler.popPush("particles");

                    profiler.push("submit particles");
                    ParticlesRenderState state = lrse.canpipe_getParticlesRenderStates()[this.canpipe_currentShadowCascadeIdx];
                    state.submit(this.submitNodeStorage, levelRenderState.cameraRenderState);

                    profiler.popPush("render features");
                    this.featureRenderDispatcher.renderAllFeatures();

                    // state.reset();  // `ParticleGroupRenderState`s are shared
                    state.particles.clear();

                    profiler.pop();
                }

                profiler.pop();

            } finally {
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setBufferSourceOverride(null);
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setCrumblingBufferSourceOverride(null);
            }
        }

        this.canpipe_currentShadowCascadeIdx = -1;

        modelViewMatrixStack.popMatrix();

        this.minecraft.options.entityShadows().set(entityShadowsOptionValue);

        profiler.pop();
    }

    @WrapOperation(
        method = "renderLevel",
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
        this.targets.translucent = frame.importExternal("translucent", p.fabulousTargets.translucentTerrainFramebuffer());
        this.targets.itemEntity = frame.importExternal("item_entity", p.fabulousTargets.translucentItemEntityFramebuffer());
        this.targets.particles = frame.importExternal("particles", p.fabulousTargets.translucentParticlesFramebuffer());
        this.targets.weather = frame.importExternal("weather", p.fabulousTargets.weatherFramebuffer());
        this.targets.clouds = frame.importExternal("clouds", p.fabulousTargets.cloudsFramebuffer());

        return null;
    }

    @WrapMethod(method = "extractVisibleEntities")
    void suppressEntityShadows(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState output, Operation<Void> operation) {
        Pipeline p = Pipelines.getCurrent();
        boolean disableEntityShadows = p != null && p.shadows != null;

        boolean originalEntityShadowsOptionValue = this.minecraft.options.entityShadows().get();

        try {
            if (disableEntityShadows) {
                this.minecraft.options.entityShadows().set(false);
            }
            operation.call(camera, frustum, deltaTracker, output);
        }
        finally {
            if (disableEntityShadows) {
                this.minecraft.options.entityShadows().set(originalEntityShadowsOptionValue);
            }
        }
    }

    @WrapOperation(
        method = "offsetFrustum",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/culling/Frustum;offsetToFullyIncludeCameraCube("+
                "I"+
            ")Lnet/minecraft/client/renderer/culling/Frustum;"
        )
    )
    private static Frustum dontOffsetShadowFrustum(Frustum frustum, int size, Operation<Frustum> operation) {
        var mc = Minecraft.getInstance();
        if (((LevelRendererExtended) mc.levelRenderer).canpipe_getCurrentShadowCascadeIdx() >= 0) {
            return frustum;
        }
        return operation.call(frustum, size);
    }

    @ModifyExpressionValue(
        method = "extractVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;isDetached()Z"
        )
    )
    private boolean addPlayerWhenCollectingVisibleEntities(boolean original) {
        return original || this.canpipe_currentShadowCascadeIdx >= 0;
    }

    @WrapOperation(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createSampler("+
                "Lcom/mojang/blaze3d/textures/AddressMode;"+
                "Lcom/mojang/blaze3d/textures/AddressMode;"+
                "Lcom/mojang/blaze3d/textures/FilterMode;"+
                "Lcom/mojang/blaze3d/textures/FilterMode;"+
                "I"+
                "Ljava/util/OptionalDouble;"+
            ")Lcom/mojang/blaze3d/textures/GpuSampler;"
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
        method = {"clearVisibleSections", "applyFrustum", "prepareChunkRenders"},
        require = 3,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            opcode = Opcodes.GETFIELD
        )
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (this.canpipe_currentShadowCascadeIdx >= 0) {
            return this.canpipe_visibleSections[this.canpipe_currentShadowCascadeIdx];
        }
        return original;
    }

    @ModifyExpressionValue(
        method = {"clearVisibleSections", "applyFrustum"},
        require = 2,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;nearbyVisibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            opcode = Opcodes.GETFIELD
        )
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (this.canpipe_currentShadowCascadeIdx >= 0) {
            return this.canpipe_nearbyVisibleSectionsSink;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = {"extractVisibleEntities", "submitEntities"},
        require = 2,
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;entityRenderStates:Ljava/util/List;",
            opcode = Opcodes.GETFIELD
        )
    )
    List<EntityRenderState> replaceEntityRenderStates(List<EntityRenderState> entityRenderStates) {
        if (this.canpipe_currentShadowCascadeIdx >= 0) {
            return ((LevelRenderStateExtended) this.levelRenderState).canpipe_getEntityRenderStates()[this.canpipe_currentShadowCascadeIdx];
        }
        return entityRenderStates;
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass("+
                "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
                "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"+
                "Lorg/joml/Matrix4fc;"+
            ")V"
        )
    )
    void beforeLateDebugPass(CallbackInfo ci, @Local FrameGraphBuilder frame) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        FramePass pass = frame.addPass("can-pipe fabulous passes");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> {
            ((GameRendererExtended) this.minecraft.gameRenderer).canpipe_setOriginType(2);  // camera
            p.onAfterWorldRender();
        });
    }

}
