package fewizz.canpipe.mixin;

import java.util.OptionalDouble;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
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
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.b3d.CommandEncoderBackendExtended;
import fewizz.canpipe.helpers.PerVertexFormatBufferSource;
import fewizz.canpipe.helpers.ShadowFrustum;
import fewizz.canpipe.mixininterface.FeatureRenderDispatcherExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.MinecraftExtended;
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
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.LightLayer;


@Mixin(value = LevelRenderer.class, priority = 1001)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private SubmitNodeStorage submitNodeStorage;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private ParticlesRenderState particlesRenderState;

    @Shadow @Final private LevelTargetBundle targets = new LevelTargetBundle();
    @Shadow @Final private RenderBuffers renderBuffers;

    @Shadow abstract ChunkSectionsToRender prepareChunkRenders(Matrix4fc matrix4fc, double d, double e, double f);
    @Shadow private void checkPoseStack(PoseStack poseStack) {}
    @Shadow private void cullTerrain(Camera camera, Frustum frustum, boolean bl) {}
    @Shadow private void extractVisibleEntities(Camera camera, Frustum frustum, DeltaTracker deltaTracker, LevelRenderState levelRenderState) {}
    @Shadow private void extractVisibleBlockEntities(Camera camera, float f, LevelRenderState levelRenderState) {}
    @Shadow private void submitBlockEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeStorage submitNodeStorage) {}
    @Shadow private void submitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector) {}
    @Shadow private void applyFrustum(Frustum frustum) {}

    @Unique volatile private boolean canpipe_isRenderingShadows = false;
    @Unique private int canpipe_shadowCascade = 0;
    @Unique private int canpipe_originType = 0;
    @Unique private float canpipe_eyeBlockLight = 0.0F;
    @Unique private float canpipe_eyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedEyeBlockLight = 0.0F;
    @Unique private float canpipe_smoothedEyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedRainGradient = 0.0F;
    @Unique private float canpipe_smoothedThunderGradient = 0.0F;
    @Unique @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> canpipe_visibleSections = new ObjectArrayList<>(10000);
    @Unique @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> canpipe_nearbyVisibleSections = new ObjectArrayList<>(50);
    @Unique @Final private PerVertexFormatBufferSource canpipe_perVertexFormetBufferSource = new PerVertexFormatBufferSource();

    @Override public boolean canpipe_getIsRenderingShadows() { return this.canpipe_isRenderingShadows; }
    @Override public int canpipe_getShadowCascade() { return this.canpipe_shadowCascade; }
    @Override public float canpipe_getEyeBlockLight() { return this.canpipe_eyeBlockLight; }
    @Override public float canpipe_getEyeSkyLight() { return this.canpipe_eyeSkyLight; }
    @Override public float canpipe_getSmoothedEyeBlockLight() { return this.canpipe_smoothedEyeBlockLight; }
    @Override public float canpipe_getSmoothedEyeSkyLight() { return this.canpipe_smoothedEyeSkyLight; }
    @Override public float canpipe_getSmoothedRainGradient() { return this.canpipe_smoothedRainGradient; }
    @Override public float canpipe_getSmoothedThunderGradient() { return this.canpipe_smoothedThunderGradient; }
    @Override public int canpipe_getOriginType() { return this.canpipe_originType; }
    @Override public void canpipe_setOriginType(int originType) { this.canpipe_originType = originType; }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I"
        )
    )
    void renderShadowsAfterLightUpdates(
        GraphicsResourceAllocator graphicsResourceAllocator,
        DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        Camera camera,
        Matrix4f viewMatrix,
        Matrix4f projectionMatrix,
        Matrix4f cullMatrix,
        GpuBufferSlice gpuBufferSlice,
        Vector4f clearColor,
        boolean renderSky,
        CallbackInfo ci
    ) throws Exception {
        Pipeline p = Pipelines.getCurrent();

        if (p == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float pt = deltaTracker.getGameTimeDeltaPartialTick(false);

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
            this.canpipe_eyeBlockLight > this.canpipe_smoothedEyeBlockLight && !p.smoothBrightnessBidirectionaly
            ? this.canpipe_eyeBlockLight
            : Mth.lerp(brightnessDelta, this.canpipe_smoothedEyeBlockLight, this.canpipe_eyeBlockLight);

        this.canpipe_smoothedEyeSkyLight =
            this.canpipe_eyeSkyLight > this.canpipe_smoothedEyeSkyLight && !p.smoothBrightnessBidirectionaly
            ? this.canpipe_eyeSkyLight
            : Mth.lerp(brightnessDelta, this.canpipe_smoothedEyeSkyLight, this.canpipe_eyeSkyLight);

        this.canpipe_smoothedRainGradient = Mth.lerp(rainDelta, this.canpipe_smoothedRainGradient, mc.level.getRainLevel(pt));
        this.canpipe_smoothedThunderGradient = Mth.lerp(thunderDelta, this.canpipe_smoothedThunderGradient, mc.level.getThunderLevel(pt));

        if (p.shadows == null || !mc.level.dimensionType().hasSkyLight()) {
            return;
        }

        this.canpipe_isRenderingShadows = true;

        Profiler.get().popPush("can-pipe shadows");
        Profiler.get().push("preparations");

        GameRendererExtended gre = ((GameRendererExtended) mc.gameRenderer);
        Vector3f toSunDir = p.getSunOrMoonDir(mc.level, new Vector3f());

        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.mul(viewMatrix);

        boolean prevEntityShadows = mc.options.entityShadows().get();
        mc.options.entityShadows().set(false);

        PoseStack poseStack = new PoseStack();

        CommandEncoderBackendExtended commandEncoder = (CommandEncoderBackendExtended) RenderSystem.getDevice().createCommandEncoder();

        GpuTexture shadowTexture = p.shadows.framebuffers().get(0).getDepthTexture();
        commandEncoder.canpipe_clearDepthTexture(
            shadowTexture,
            1.0,
            0, shadowTexture.getMipLevels(),
            0, shadowTexture.getDepthOrLayers()
        );

        for (this.canpipe_shadowCascade = 0; this.canpipe_shadowCascade < p.shadows.cascadeRadii().size()+1; ++this.canpipe_shadowCascade) {
            Profiler.get().popPush("cascade " + this.canpipe_shadowCascade);

            Frustum shadowFrustum = new ShadowFrustum(
                Uniforms.FRX_SHADOW_VIEW_MATRIX, gre.canpipe_getShadowProjectionMatrices()[this.canpipe_shadowCascade],
                gre.canpipe_getShortenedViewProjectionMatrices()[this.canpipe_shadowCascade], toSunDir
            );
            shadowFrustum.prepare(camera.position().x, camera.position().y, camera.position().z);

            Profiler.get().push("apply frustum");

            applyFrustum(shadowFrustum);

            RenderTarget originalMainRenderTarget = mc.getMainRenderTarget();

            try {
                ((MinecraftExtended) mc).canpipe_setMainRenderTargetOverride(p.shadows.framebuffers().get(this.canpipe_shadowCascade));
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setBufferSourceOverride(this.canpipe_perVertexFormetBufferSource);

                Profiler.get().popPush("render sections");
                ChunkSectionsToRender chunkSectionsToRender = this.prepareChunkRenders(viewMatrix, camera.position().x, camera.position().y, camera.position().z);
                chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                chunkSectionsToRender.chunkSectionInfos();

                if (p.shadows.allowEntities()) {
                    Profiler.get().popPush("entities");

                    Profiler.get().push("extract entities");
                    this.extractVisibleEntities(camera, shadowFrustum, deltaTracker, this.levelRenderState);

                    Profiler.get().popPush("extract block entities");
                    this.extractVisibleBlockEntities(camera, pt, this.levelRenderState);

                    Profiler.get().popPush("submit entities");
                    this.submitEntities(poseStack, levelRenderState, this.submitNodeStorage);

                    Profiler.get().popPush("submit block entities");
                    this.submitBlockEntities(poseStack, levelRenderState, this.submitNodeStorage);

                    Profiler.get().popPush("render features");
                    this.featureRenderDispatcher.renderAllFeatures();
                    this.checkPoseStack(poseStack);

                    Profiler.get().popPush("per-vertex format end batch");
                    this.canpipe_perVertexFormetBufferSource.endBatch();

                    Profiler.get().popPush("end batch");
                    this.renderBuffers.bufferSource().endBatch();

                    Profiler.get().pop();
                }

                if (p.shadows.allowParticles()) {
                    Profiler.get().popPush("particles");

                    Profiler.get().push("extract");
                    mc.particleEngine.extract(this.particlesRenderState, shadowFrustum, camera, pt);

                    Profiler.get().popPush("submit particles");
                    this.particlesRenderState.submit(this.submitNodeStorage, this.levelRenderState.cameraRenderState);

                    Profiler.get().popPush("render features");
                    this.featureRenderDispatcher.renderAllFeatures();
                    this.particlesRenderState.reset();

                    Profiler.get().popPush("end batch");
                    this.renderBuffers.bufferSource().endBatch();

                    Profiler.get().pop();
                }

            } finally {
                ((MinecraftExtended) mc).canpipe_setMainRenderTargetOverride(originalMainRenderTarget);
                ((FeatureRenderDispatcherExtended) this.featureRenderDispatcher).canpipe_setBufferSourceOverride(null);
            }

            Profiler.get().pop();
            this.levelRenderState.reset();
        }

        this.canpipe_shadowCascade = 0;

        modelViewMatrixStack.popMatrix();

        mc.options.entityShadows().set(prevEntityShadows);
        this.canpipe_isRenderingShadows = false;

        Profiler.get().pop();
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
        Operation<PostChain> opration,
        @Local RenderTargetDescriptor renderTargetDescriptor,
        @Local FrameGraphBuilder frameGraphBuilder
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return opration.call(instance);  // Initialise transparency post chain normally
        }
        // Don't create transparency post chain, will be handled by pipeline

        this.targets.main = frameGraphBuilder.importExternal("main", p.solidFramebuffer);
        this.targets.translucent = frameGraphBuilder.importExternal("translucent", p.translucentTerrainFramebuffer);
        this.targets.itemEntity = frameGraphBuilder.importExternal("item_entity", p.translucentItemEntityFramebuffer);
        this.targets.particles = frameGraphBuilder.importExternal("particles", p.particlesFramebuffer);
        this.targets.weather = frameGraphBuilder.importExternal("weather", p.weatherFramebuffer);
        this.targets.clouds = frameGraphBuilder.importExternal("clouds", p.cloudsFramebuffer);

        return null;
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
        if (((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
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
        return this.canpipe_isRenderingShadows ? true : original;
    }

    @WrapOperation(
        method = {
            "method_62214",  // Fabric
            "lambda$addMainPass$1"  // NeoForge
        },
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
            return operation.call(device, u, v, FilterMode.NEAREST, FilterMode.NEAREST, 1, OptionalDouble.empty());
        }
        return operation.call(device, u, v, min, mag, maxAnisotropy, maxLod);
    }

    @ModifyExpressionValue(
        method = {"clearVisibleSections", "applyFrustum", "prepareChunkRenders"},
        at = @At(
            value= "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
        ),
        require = 3
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (((LevelRendererExtended) this).canpipe_getIsRenderingShadows()) {
            return this.canpipe_visibleSections;
        }
        return original;
    }

    @ModifyExpressionValue(
        method = {"clearVisibleSections", "applyFrustum"},
        at = @At(
            value= "FIELD",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;nearbyVisibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
        ),
        require = 2
    )
    ObjectArrayList<SectionRenderDispatcher.RenderSection> replaceNearbyVisibleSections(ObjectArrayList<SectionRenderDispatcher.RenderSection> original) {
        if (((LevelRendererExtended) this).canpipe_getIsRenderingShadows()) {
            return this.canpipe_nearbyVisibleSections;
        }
        return original;
    }

}
