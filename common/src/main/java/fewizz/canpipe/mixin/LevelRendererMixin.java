package fewizz.canpipe.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.GFX;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.ProgramBase;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtended {

    @Final
    @Shadow
    private List<Entity> visibleEntities;

    @Shadow
    @Final
    private LevelTargetBundle targets = new LevelTargetBundle();

    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    @Unique
    public Matrix4f canpipe_shadowViewMatrix = new Matrix4f();
    @Unique
    public Matrix4f[] canpipe_shadowProjectionMatrices = new Matrix4f[] { new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f() };
    @Unique
    public Vector4f[] canpipe_shadowCenters = new Vector4f[] { new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f() };
    @Unique
    volatile public boolean canpipe_isRenderingShadows = false;

    @Override
    public boolean canpipe_getIsRenderingShadows() {
        return this.canpipe_isRenderingShadows;
    }

    @Override
    public Matrix4f canpipe_getShadowViewMatrix() {
        return this.canpipe_shadowViewMatrix;
    }

    @Override
    public Matrix4f[] canpipe_getShadowProjectionMatrices() {
        return this.canpipe_shadowProjectionMatrices;
    }

    @Override
    public Vector4f[] canpipe_getShadowCenters() {
        return this.canpipe_shadowCenters;
    }

    @Shadow
    abstract void checkPoseStack(PoseStack poseStack);

    @Shadow
    abstract void renderSectionLayer(RenderType renderType, double x, double y, double z, Matrix4f viewMatrix, Matrix4f projectionMatrix);

    @Shadow
    abstract void setupRender(Camera camera, Frustum frustum, boolean frustumWasAlreadyCaptured, boolean inSpectatorMode);

    @Shadow
    abstract boolean collectVisibleEntities(Camera camera, Frustum frustum, List<Entity> list);

    @Shadow
    abstract void renderEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Camera camera, DeltaTracker deltaTracker, List<Entity> list);

    @Shadow
    abstract void compileSections(Camera camera);

    @Shadow
    abstract void applyFrustum(Frustum frustum);

    @Shadow
    abstract void renderBlockEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, MultiBufferSource.BufferSource bufferSource2, Camera camera, float f);

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
        boolean shouldRenderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        Matrix4f modelViewMatrix,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        Pipeline p = Pipelines.getCurrent();

        if (p == null || p.skyShadows == null) {
            return;
        }

        this.canpipe_isRenderingShadows = true;

        Minecraft mc = Minecraft.getInstance();
        GameRendererAccessor gra = ((GameRendererAccessor) mc.gameRenderer);
        float renderDistance = mc.gameRenderer.getRenderDistance();
        Vector3f toSunDir = p.getSunOrMoonDir(mc.level, new Vector3f());
        Vector3f fromSunDir = toSunDir.negate(new Vector3f());

        var camPos = camera.getPosition();
        var sunPosOffset = new Vec3(toSunDir.mul(renderDistance + 32, new Vector3f()));
        var sunPos = camPos.add(sunPosOffset);

        canpipe_shadowViewMatrix.setLookAt(
            sunPosOffset.toVector3f(),       // eye pos
            new Vector3f(0.0F, 0.0F, 0.0F),  // center
            new Vector3f(0.0F, 1.0F, 0.0F)   // up
        );

        Camera shadowCamera = new Camera() {{
            setPosition(sunPos);
            setRotation(
                (float) Math.toDegrees(Math.atan2(-fromSunDir.x, fromSunDir.z)),
                (float) Math.toDegrees(Math.atan2(-fromSunDir.y, Math.sqrt(fromSunDir.x*fromSunDir.x + fromSunDir.z*fromSunDir.z)))
            );
            ((CameraAccessor)(Object) this).canpipe_setDetached(true);
            ((CameraAccessor)(Object) this).canpipe_setEntity(camera.getEntity());
        }};

        Framebuffer shadowFramebuffer = p.framebuffers.get(p.skyShadows.framebufferName());

        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.mul(modelViewMatrix);

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        MultiBufferSource.BufferSource crumblingBufferSource = this.renderBuffers.crumblingBufferSource();

        RenderSystem.enablePolygonOffset();
        RenderSystem.polygonOffset(
            p.skyShadows.offsetSlopeFactor(),
            p.skyShadows.offsetBiasUnits()
        );

        shadowFramebuffer.bindWrite(true /* update viewport */);

        for (int cascade = 0; cascade < 4; ++cascade) {
            p.shadowPrograms.get(CanPipe.VertexFormats.BLOCK).FRXU_CASCADE.set(cascade);

            float cascadeRenderDistance;

            if (cascade == 0) {
                cascadeRenderDistance = renderDistance + 32;
            }
            else {
                cascadeRenderDistance = p.skyShadows.cascadeRadii().get(cascade-1) / 2.0F;
            }

            gra.canpipe_setDepthFar(Optional.of(cascadeRenderDistance));
            float fov = (float) mc.options.fov().get().intValue();
            Matrix4f cascadeProjectionMatrix = mc.gameRenderer.getProjectionMatrix(fov);
            gra.canpipe_setDepthFar(Optional.empty());

            Frustum cascadeFrustum = new Frustum(modelViewMatrix, cascadeProjectionMatrix);

            Vector4f min = new Vector4f(+Float.MAX_VALUE);
            Vector4f max = new Vector4f(-Float.MAX_VALUE);

            for (Vector4f point : cascadeFrustum.getFrustumPoints()) {
                point.mul(canpipe_shadowViewMatrix);
                min.min(point);
                max.max(point);
            }

            Vector4f center = new Vector4f().add(min).add(max).div(2.0F);

            float effectiveRadius = Math.max(max.x - min.x, max.y - min.y) / 2.0F;

            canpipe_shadowProjectionMatrices[cascade].setOrtho(
                center.x - effectiveRadius,  // left
                center.x + effectiveRadius,  // right
                center.y - effectiveRadius,  // bottom
                center.y + effectiveRadius,  // up
                0.0F,                        // near
                -center.z + effectiveRadius  // far
            );
            canpipe_shadowCenters[cascade].set(center.x, center.y, center.z, effectiveRadius);

            Frustum shadowFrustum = new Frustum(
                canpipe_shadowViewMatrix,
                canpipe_shadowProjectionMatrices[cascade]
            );
            shadowFrustum.prepare(camPos.x, camPos.y, camPos.z);

            if (cascade == 0) {
                this.setupRender(new Camera() {{
                    setPosition(camPos);
                    setRotation(shadowCamera.getYRot(), shadowCamera.getXRot());
                }}, shadowFrustum, false, false);
                this.compileSections(camera);
            }
            else {
                this.applyFrustum(shadowFrustum);
            }

            GFX.glFramebufferTextureLayer(GL33C.GL_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT, shadowFramebuffer.depthAttachment.texture().getId(), 0, cascade);

            RenderSystem.disableCull();  // Light can pass through chunk edge. Not ideal solution
            this.renderSectionLayer(RenderType.solid(), camPos.x, camPos.y, camPos.z, modelViewMatrix, projectionMatrix);
            this.renderSectionLayer(RenderType.cutoutMipped(), camPos.x, camPos.y, camPos.z, modelViewMatrix, projectionMatrix);
            this.renderSectionLayer(RenderType.cutout(), camPos.x, camPos.y, camPos.z, modelViewMatrix, projectionMatrix);
            RenderSystem.enableCull();
        }

        Frustum shadowFrustum = new Frustum(
            canpipe_shadowViewMatrix,
            canpipe_shadowProjectionMatrices[0]
        );
        shadowFrustum.prepare(camPos.x, camPos.y, camPos.z);

        this.collectVisibleEntities(shadowCamera, shadowFrustum, this.visibleEntities);
        this.renderEntities(poseStack, bufferSource, camera, deltaTracker, this.visibleEntities);
        this.renderBlockEntities(poseStack, bufferSource, crumblingBufferSource, camera, deltaTracker.getGameTimeDeltaPartialTick(false));
        this.checkPoseStack(poseStack);
        this.visibleEntities.clear();
        bufferSource.endBatch();
        crumblingBufferSource.endBatch();

        RenderSystem.disablePolygonOffset();

        modelViewMatrixStack.popMatrix();
        mc.mainRenderTarget.bindWrite(true);

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

    @Inject(
        method = "renderSectionLayer",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;bind()V"
        )
    )
    void beforeSectionLayerDraw(
        CallbackInfo ci,
        @Local CompiledShaderProgram program,
        @Local SectionRenderDispatcher.RenderSection section
    ) {
        if (program instanceof ProgramBase pb) {
            if (pb.FRX_MODEL_TO_WORLD != null) {
                BlockPos pos = section.getOrigin();
                pb.FRX_MODEL_TO_WORLD.set(pos.getX(), pos.getY(),pos.getZ(), 1.0F);
                pb.FRX_MODEL_TO_WORLD.upload();
                pb.FRX_MODEL_TO_WORLD.set(0.0F, 0.0F, 0.0F, 1.0F);
            }
            if (pb.CANPIPE_ORIGIN_TYPE != null) {
                pb.CANPIPE_ORIGIN_TYPE.set(1); // region
                pb.CANPIPE_ORIGIN_TYPE.upload();
                pb.CANPIPE_ORIGIN_TYPE.set(0);  // camera
            }
        }
    }

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
        index = 0
    )
    boolean disableSmartCullIfShadow(boolean original) {
        return canpipe_isRenderingShadows ? false : original;
    }

    /*@ModifyExpressionValue(
        method = "setupRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;consumeFrustumUpdate()Z"
        )
    )
    boolean alwaysApplyFrustumIfShadow(boolean consumeFrustumUpdate) {
        return canpipe_isRenderingShadow ? true : consumeFrustumUpdate;
    }*/

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
        if (((LevelRendererExtended)mc.levelRenderer).canpipe_getIsRenderingShadows()) {
            return frustum;
        }
        return original.call(frustum, size);
    }

    @ModifyArg(
        method = "collectVisibleEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setViewScale(D)V"
        ),
        index = 0
    )
    double fixEntityRenderDistance(double original) {  // TODO use optimal value
        return this.canpipe_isRenderingShadows ? Minecraft.getInstance().gameRenderer.getRenderDistance() * 2.0 : original;
    }

}
