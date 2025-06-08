package fewizz.canpipe.mixin.m03_core;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;
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
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.GFX;
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
    ) {
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

        Profiler.get().popPush("canpipe_shadows");
        this.canpipe_isRenderingShadows = true;

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

        PoseStack poseStack = new PoseStack();

        RenderTarget originalMainRenderTarget = mc.mainRenderTarget;

        Framebuffer shadowFramebuffer = p.shadows.framebuffer();
        mc.mainRenderTarget = shadowFramebuffer;

        boolean prevEntityShadows = mc.options.entityShadows().get();
        mc.options.entityShadows().set(false);

        try {

        shadowFramebuffer.bindAndClearFully();

        for (int cascade = 0; cascade < 4; ++cascade) {
            Profiler.get().popPush("cascade " +cascade);

            Uniforms.FRXU_CASCADE.value = cascade;

            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM.size());
                Uniforms.MATERIAL_PROGRAM.writeTo(builder);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
            }

            Frustum shadowFrustum = new ShadowFrustum(
                gre.canpipe_getShadowViewMatrix(), gre.canpipe_getShadowProjectionMatrices()[cascade],
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

            Profiler.get().popPush("render sections");

            GlStateManager._glBindFramebuffer(GL33C.GL_FRAMEBUFFER, shadowFramebuffer.glID());
            GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, shadowFramebuffer.depthAttachment.texture().glId(), 0, cascade);

            ChunkSectionsToRender chunkSectionsToRender = this.prepareChunkRenders(viewMatrix, camPos.x, camPos.y, camPos.z);
			chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.OPAQUE);
            chunkSectionsToRender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT);

            Profiler.get().popPush("collect entities");

            this.collectVisibleEntities(camera, shadowFrustum, this.visibleEntities);

            MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();

            if (p.shadows.allowEntities()) {
                Profiler.get().popPush("render entities");

                this.renderEntities(poseStack, bufferSource, camera, deltaTracker, this.visibleEntities);
                this.renderBlockEntities(poseStack, bufferSource, bufferSource, camera, deltaTracker.getGameTimeDeltaPartialTick(false));
                this.checkPoseStack(poseStack);
                this.visibleEntities.clear();
                bufferSource.endBatch();
            }

            if (p.shadows.allowParticles()) {
                Profiler.get().popPush("render particles");
                mc.particleEngine.render(camera, pt, this.renderBuffers.bufferSource());
            }

            Profiler.get().pop();
        }

        Uniforms.FRXU_CASCADE.value = 0;

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            var builder = Std140Builder.onStack(memoryStack, Uniforms.MATERIAL_PROGRAM.size());
            Uniforms.MATERIAL_PROGRAM.writeTo(builder);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(Uniforms.MATERIAL_PROGRAM_UBO.slice(), builder.get());
        }

        modelViewMatrixStack.popMatrix();

        } finally {
            mc.mainRenderTarget = originalMainRenderTarget;
            mc.options.entityShadows().set(prevEntityShadows);
        }

        Profiler.get().pop();

        this.canpipe_isRenderingShadows = false;
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

    /*@Inject(
        method = {
            "method_68480",  // fabric
            "lambda$renderSectionLayer$11"  // neoforge
        },
        at = @At("TAIL")
    )
    private static void onRenderSectionLayerUniformUpload(
        BlockPos pos, double x, double y, double z,
        UniformUploader uniformUploader,
        CallbackInfo ci
    ) {
        // uniformUploader.upload("frx_modelToWorld", pos.getX(), pos.getY(),pos.getZ(), 1.0F);
    }*/

    /*@Inject(
        method = "renderSectionLayer",
        at = @At("RETURN")
    )
    void afterAllSectionsRendered(CallbackInfo ci) {
        CanPipe.GlobalState.originType = 0; // camera
    }*/

    @ModifyArg(
        method = "setupRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;update("+
                "Z"+
                "Lnet/minecraft/client/Camera;"+
                "Lnet/minecraft/client/renderer/culling/Frustum;"+
                "Ljava/util/List;"+
                "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;"+
            ")V"
        ),
        index = 0,
        require = 0  // sodium @Overwrite s this method
    )
    private boolean disableSmartCullIfShadow(boolean original) {
        return canpipe_isRenderingShadows ? false : original;
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

    // Code lower moves bufferSource.endBatch(Sheets.translucentItemSheet());
    // after main.get().copyDepthFrom(translucent.get());
    // if pipeline is active,
    // because we render items into translucent framebuffer
    /*@ModifyExpressionValue(
        method = {  // lambda in the `addMainPass`
            "method_62214",  // fabric
            "lambda$addMainPass$2"  // neoforge
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;translucentItemSheet()Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private RenderType dontDrawTranslucentIteims(RenderType original) {
        if (Pipelines.getCurrent() != null) {
            // solid should already be rendered, so nothing should happen (:clueless:),
            // items will be rendered later, right before translucent terrain
            // (see next @Inject)
            return RenderType.solid();
        }
        return original;
    }

    @Inject(
        method = {"method_62214", "lambda$addMainPass$2"},  // lambda in the `addMainPass`
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V",
            ordinal = 1
        ),
        cancellable = true
    )
    private void drawTranslucentItemsRightBeforeTranslucentTerrain(CallbackInfo ci) {
        if (Pipelines.getCurrent() == null) {
            return;
        }
        this.targets.itemEntity.get().copyDepthFrom(this.targets.main.get());
        this.renderBuffers.bufferSource().endBatch(Sheets.translucentItemSheet());
    }

    @WrapOperation(
        method = {"method_62214", "lambda$addMainPass$2"},  // lambda in the `addMainPass`
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;copyDepthFrom(Lcom/mojang/blaze3d/pipeline/RenderTarget;)V",
            ordinal = 1  // 0 - is for items entities, 1 - translucent
        ),
        remap = false
    )
    private void dontOverwriteTranslucentDepth(RenderTarget instance, RenderTarget other, Operation<Void> original) {
        // if translucent == itemEntity, then no need to overwrite depth (right?)
        if (instance == targets.translucent.get() && targets.translucent.get() == targets.itemEntity.get()) {
            return;
        }
        original.call(instance, other);
    }*/

}
