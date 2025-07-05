package fewizz.canpipe.mixin;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
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
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.helpers.ShadowFrustum;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

@Mixin(value = LevelRenderer.class, priority = 1001)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Shadow @Final private List<Entity> visibleEntities;
    @Shadow @Final private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    @Shadow @Final private LevelTargetBundle targets = new LevelTargetBundle();
    @Shadow @Final private RenderBuffers renderBuffers;

    @Shadow abstract ChunkSectionsToRender prepareChunkRenders(Matrix4fc matrix4fc, double d, double e, double f);
    @Shadow private void checkPoseStack(PoseStack poseStack) {}
    @Shadow private void setupRender(Camera camera, Frustum frustum, boolean frustumWasAlreadyCaptured, boolean inSpectatorMode) {}
    @Shadow private boolean collectVisibleEntities(Camera camera, Frustum frustum, List<Entity> list) { return false; }
    @Shadow private void renderEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list) {}
    @Shadow private void applyFrustum(Frustum frustum) {}
    @Shadow private void renderBlockEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, MultiBufferSource.BufferSource bufferSource2, Camera camera, float f) {}

    @Unique volatile private boolean canpipe_isRenderingShadows = false;
    @Unique private float canpipe_eyeBlockLight = 0.0F;
    @Unique private float canpipe_eyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedEyeBlockLight = 0.0F;
    @Unique private float canpipe_smoothedEyeSkyLight = 0.0F;
    @Unique private float canpipe_smoothedRainGradient = 0.0F;
    @Unique private float canpipe_smoothedThunderGradient = 0.0F;

    @Override public boolean canpipe_getIsRenderingShadows() { return this.canpipe_isRenderingShadows; }
    @Override public float canpipe_getEyeBlockLight() { return this.canpipe_eyeBlockLight; }
    @Override public float canpipe_getEyeSkyLight() { return this.canpipe_eyeSkyLight; }
    @Override public float canpipe_getSmoothedEyeBlockLight() { return this.canpipe_smoothedEyeBlockLight; }
    @Override public float canpipe_getSmoothedEyeSkyLight() { return this.canpipe_smoothedEyeSkyLight; }
    @Override public float canpipe_getSmoothedRainGradient() { return this.canpipe_smoothedRainGradient; }
    @Override public float canpipe_getSmoothedThunderGradient() { return this.canpipe_smoothedThunderGradient; }

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

        if (p.shadows == null) {
            return;
        }

        this.canpipe_isRenderingShadows = true;

        Profiler.get().popPush("canpipe_shadows");
        Profiler.get().push("preparations");

        GameRendererExtended gre = ((GameRendererExtended) mc.gameRenderer);
        float renderDistance = mc.gameRenderer.getRenderDistance();
        Vector3f toSunDir = p.getSunOrMoonDir(mc.level, new Vector3f(), pt);
        Vector3f fromSunDir = toSunDir.negate(new Vector3f());

        var camPos = camera.getPosition();
        var sunPosOffset = new Vec3(toSunDir.mul(renderDistance + 48, new Vector3f()));
        var sunPos = camPos.add(sunPosOffset);

        Camera shadowCamera = new Camera() {{
            setPosition(sunPos);
            setRotation(
                (float) Math.toDegrees(Math.atan2(-fromSunDir.x, fromSunDir.z)),
                (float) Math.toDegrees(Math.atan2(-fromSunDir.y, Math.sqrt(fromSunDir.x*fromSunDir.x + fromSunDir.z*fromSunDir.z)))
            );
            ((CameraAccessor)(Object) this).canpipe_setDetached(true);
            ((CameraAccessor)(Object) this).canpipe_setEntity(camera.getEntity());
        }};

        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.mul(viewMatrix);

        boolean prevEntityShadows = mc.options.entityShadows().get();
        mc.options.entityShadows().set(false);

        PoseStack poseStack = new PoseStack();

        for (int cascade = 0; cascade < p.shadows.cascadeRadii().size()+1; ++cascade) {
            Profiler.get().popPush("cascade " +cascade);

            Uniforms.FRXU_CASCADE.set(cascade);

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM.size());
                Uniforms.MATERIAL_PROGRAM.writeTo(builder);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
            }

            Frustum shadowFrustum = new ShadowFrustum(
                Uniforms.FRX_SHADOW_VIEW_MATRIX, gre.canpipe_getShadowProjectionMatrices()[cascade],
                gre.canpipe_getShortenedViewProjectionMatrices()[cascade], camera, toSunDir
            );

            if (cascade == 0) {
                Profiler.get().push("setupRender");
                this.setupRender(new Camera() {{
                    setPosition(camPos);
                    setRotation(shadowCamera.getYRot(), shadowCamera.getXRot());
                }}, shadowFrustum, false, false);
            }
            else {
                Profiler.get().push("applyFrustum");
                this.applyFrustum(shadowFrustum);
            }

            RenderTarget originalMainRenderTarget = mc.mainRenderTarget;

            try {
                Framebuffer shadowFramebuffer = p.shadows.framebuffers().get(cascade);
                mc.mainRenderTarget = shadowFramebuffer;
                // shadowFramebuffer.bindAndClearFully();

                Profiler.get().popPush("render sections");
                ChunkSectionsToRender chunkSectionsToRender = this.prepareChunkRenders(viewMatrix, camPos.x, camPos.y, camPos.z);
                chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE);
                chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT);

                MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

                if (p.shadows.allowEntities()) {
                    Profiler.get().popPush("collect entities");
                    this.collectVisibleEntities(camera, shadowFrustum, this.visibleEntities);

                    Profiler.get().popPush("render entities");

                    this.renderEntities(poseStack, bufferSource, camera, deltaTracker, this.visibleEntities);
                    this.renderBlockEntities(poseStack, bufferSource, bufferSource, camera, deltaTracker.getGameTimeDeltaPartialTick(false));
                    this.checkPoseStack(poseStack);
                    this.visibleEntities.clear();
                }

                if (p.shadows.allowParticles()) {
                    Profiler.get().popPush("render particles");
                    mc.particleEngine.render(camera, pt, bufferSource);
                }

                bufferSource.endBatch();
            } finally {
                mc.mainRenderTarget = originalMainRenderTarget;
            }

            Profiler.get().pop();
        }

        Uniforms.FRXU_CASCADE.set(0);

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }

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
        Operation<PostChain> original,
        @Local RenderTargetDescriptor renderTargetDescriptor,
        @Local FrameGraphBuilder frameGraphBuilder
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return original.call(instance);  // Initialise transparency post chain normally
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
    private static Frustum dontOffsetShadowFrustum(Frustum frustum, int size, Operation<Frustum> original) {
        var mc = Minecraft.getInstance();
        if (((LevelRendererExtended) mc.levelRenderer).canpipe_getIsRenderingShadows()) {
            return frustum;
        }
        return original.call(frustum, size);
    }

    @ModifyExpressionValue(
        method = "collectVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;isDetached()Z"
        )
    )
    private boolean addPlayerWhenCollectingVisibleEntities(boolean original) {
        return this.canpipe_isRenderingShadows ? true : original;
    }

}
