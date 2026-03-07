package fewizz.canpipe.mixin;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.helpers.ShadowFrustum;
import fewizz.canpipe.mixininterface.CameraExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.mixininterface.LevelRendererExtended;
import fewizz.canpipe.mixininterface.MinecraftExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererExtended {

    @Shadow @Final Minecraft minecraft;
    @Shadow @Final private Camera mainCamera;
    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private GameRenderState gameRenderState;

    @Unique private long canpipe_renderStartNano = -1;
    @Unique private int canpipe_renderTarget = -1;
    @Unique private Vector3f[] canpipe_shadowInnerOffsets = null;
    @Unique private ShadowFrustum[] canpipe_shadowFrustums = null;
    @Unique private Matrix4f canpipe_worldViewMatrix = null;
    @Unique private Matrix4f canpipe_worldProjectionMatrix = null;
    @Unique private ChunkSectionsToRender[] canpipe_chunkSectionsToRender = null;

    @Override
    public void canpipe_onPipelineActivated() {
        this.canpipe_renderStartNano = System.nanoTime();

        Uniforms.CANPIPE_RENDER_FRAMES.set(-1);
        Uniforms.FRX_RENDER_SECONDS.set(0);

        Uniforms.FRX_LAST_VIEW_MATRIX.m00(Float.NEGATIVE_INFINITY);
        Uniforms.FRX_LAST_PROJECTION_MATRIX.m00(Float.NEGATIVE_INFINITY);

        Uniforms.FRX_CAMERA_POS.set(0.0F);
        Uniforms.FRX_LAST_CAMERA_POS.set(Float.NEGATIVE_INFINITY);

        Uniforms.FRX_SHADOW_VIEW_MATRIX.identity();
        this.canpipe_shadowInnerOffsets = new Vector3f[] {
            new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()
        };
        this.canpipe_shadowFrustums = new ShadowFrustum[4];
        this.canpipe_chunkSectionsToRender = new ChunkSectionsToRender[4];

        Uniforms.CANPIPE_SHADOW_CENTERS[0].set(0.0);
        Uniforms.CANPIPE_SHADOW_CENTERS[1].set(0.0);
        Uniforms.CANPIPE_SHADOW_CENTERS[2].set(0.0);
        Uniforms.CANPIPE_SHADOW_CENTERS[3].set(0.0);

        this.canpipe_worldViewMatrix = null;
        this.canpipe_worldProjectionMatrix = null;
        this.canpipe_renderTarget = -1;
    }

    @Inject(method = "resize", at = @At("HEAD"))
    void onResize(int w, int h, CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) { p.onWindowSizeChanged(w, h); }
    }

    @Inject(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;extractLevel("+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Lnet/minecraft/client/Camera;"+
                "F"+
            ")V"
        )
    )
    void beforeExtractLevel(CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        ProfilerFiller profiler = Profiler.get();
        Matrix4f viewMatrix = this.gameRenderState.levelRenderState.cameraRenderState.viewRotationMatrix;

        Uniforms.CANPIPE_RENDER_FRAMES.add(1);
        Uniforms.FRX_RENDER_SECONDS.set((float)((System.nanoTime() - this.canpipe_renderStartNano) / 1_000_000_000.0));
        Uniforms.FRX_CAMERA_POS.set(this.mainCamera.position().toVector3f());

        if (Uniforms.FRX_LAST_CAMERA_POS.get(0) == Float.NEGATIVE_INFINITY) {
            Uniforms.FRX_LAST_CAMERA_POS.set(Uniforms.FRX_CAMERA_POS);
        }

        if (p.shadows == null) { return; }

        profiler.push("can-pipe calculate shadow frustums");

        final float maxCascadeRadius = this.minecraft.options.getEffectiveRenderDistance() * 16 + 48.0F;
        final float depthTextureSize = (float) p.shadows.framebuffers().get(0).getDepthTexture().getWidth(0);

        Vector3f toSunDir = p.getSunOrMoonDir(this.minecraft.level, new Vector3f());
        Vector3f sunPosOffset = toSunDir.mul(maxCascadeRadius, new Vector3f());

        Uniforms.FRX_SHADOW_VIEW_MATRIX.setLookAt(
            sunPosOffset,                                  // eye pos
            new Vector3f(0.0F, 0.0F, 0.0F),                // center
            !(sunPosOffset.x == 0 && sunPosOffset.z == 0)  // up
                ? new Vector3f(0.0F, 1.0F, 0.0F)
                : new Vector3f(0.0F, 0.0F, 1.0F)
        );

        Uniforms.FRX_INVERSE_SHADOW_VIEW_MATRIX.set(Uniforms.FRX_SHADOW_VIEW_MATRIX).invert();

        var shadowRotationMatrix = new Matrix3f(Uniforms.FRX_SHADOW_VIEW_MATRIX);
        var inverseShadowViewMatrix = new Matrix4f(Uniforms.FRX_SHADOW_VIEW_MATRIX).invert();

        float prevCascadeRadius = -1.0F;

        // from smallest to biggest
        for (int cascade = p.shadows.cascadeRadii().size(); cascade >= 0; --cascade) {
            float cascadeRadius;
            Vector3f center;

            if (cascade == 0) {  // biggest, radius depends on render distance
                cascadeRadius = maxCascadeRadius;
            }
            else {
                cascadeRadius = p.shadows.cascadeRadii().get(cascade-1);
            }

            prevCascadeRadius = Math.max(cascadeRadius, prevCascadeRadius);

            center = new Vector3f(mainCamera.forwardVector()).mul(cascadeRadius);
            center.mulProject(Uniforms.FRX_SHADOW_VIEW_MATRIX);

            final float metersPerPixel = cascadeRadius*2.0F / depthTextureSize;

            Vector3f dPos = Uniforms.FRX_CAMERA_POS.sub(Uniforms.FRX_LAST_CAMERA_POS, new Vector3f());
            Vector3f dShadowPos = dPos.mul(shadowRotationMatrix).div(metersPerPixel);

            this.canpipe_shadowInnerOffsets[cascade].add(dShadowPos);
            this.canpipe_shadowInnerOffsets[cascade].sub(this.canpipe_shadowInnerOffsets[cascade].floor(new Vector3f()));

                            // for camera rotation                         // for position change
            center.x -= (center.x % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].x * metersPerPixel;
            center.y -= (center.y % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].y * metersPerPixel;
            center.z -= (center.z % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].z * metersPerPixel;

            Uniforms.CANPIPE_SHADOW_CENTERS[cascade].set(center.x, center.y, center.z, cascadeRadius);

            // For shortened projection matrix, from player's perspective
            // Such frustum should include whole cascade along -z
            float depthFar = 0.0F;
            for (int x = -1; x <= 1; x += 2) {  // for each cascade corner
                for (int y = -1; y <= 1; y += 2) {
                    for (int z = -1; z <= 1; z += 2) {
                        var corner = new Vector3f(x, y, z).mul(cascadeRadius).add(center)
                            .mulProject(inverseShadowViewMatrix).mulProject(viewMatrix);
                        depthFar = Math.min(Math.max(depthFar, -corner.z), maxCascadeRadius);
                    }
                }
            }

            var shortenedProjectionMatrix = ((CameraExtended) this.mainCamera).canpipe_createProjectionMatrixForCulling(depthFar);

            var shortendedViewProjectionMatrix = new Matrix4f(shortenedProjectionMatrix).mul(viewMatrix);

            Vector3f min = new Vector3f();
            Vector3f max = new Vector3f();

            new Matrix4f()
                .mul(Uniforms.FRX_SHADOW_VIEW_MATRIX)
                .mul(new Matrix4f(shortenedProjectionMatrix).mul(viewMatrix).invert())
                .frustumAabb(min, max);  // frustum AABB in shadow view space

            // those matrices aren't passed into shadow material programs,
            // no need to worry about constant radius
            var shadowProjectionMatrix = new Matrix4f().setOrtho(
                Math.max(min.x, center.x - cascadeRadius),  // left
                Math.min(max.x, center.x + cascadeRadius),  // right
                Math.max(min.y, center.y - cascadeRadius),  // bottom
                Math.min(max.y, center.y + cascadeRadius),  // up
                0.0F,                       // near
               -Math.max(min.z, center.z - cascadeRadius)   // far
            );

            ShadowFrustum shadowFrustum = new ShadowFrustum(
                Uniforms.FRX_SHADOW_VIEW_MATRIX, shadowProjectionMatrix,
                shortendedViewProjectionMatrix, toSunDir
            );
            shadowFrustum.prepare(this.mainCamera.position().x, this.mainCamera.position().y, this.mainCamera.position().z);
            this.canpipe_shadowFrustums[cascade] = shadowFrustum;
        }

        ((LevelRendererExtended) this.minecraft.levelRenderer).canpipe_prepareCascadesChunkSectionsToRender(viewMatrix, this.canpipe_chunkSectionsToRender);
        profiler.pop();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Z"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
                "Lorg/joml/Matrix4f;"+
                "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "Z"+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"+
            ")V"
        )
    )
    void beforeRenderLevel(
        DeltaTracker deltaTracker, CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f viewMatrix,
        @Local(ordinal = 1) Matrix4f projectionMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        this.canpipe_worldViewMatrix = new Matrix4f(viewMatrix);
        this.canpipe_worldProjectionMatrix = new Matrix4f(projectionMatrix);

        if (Uniforms.FRX_LAST_VIEW_MATRIX.get(0, 0) == Float.NEGATIVE_INFINITY) {
            Uniforms.FRX_LAST_VIEW_MATRIX.set(viewMatrix);
            Uniforms.FRX_LAST_PROJECTION_MATRIX.set(projectionMatrix);
        }

        Uniforms.updateFREXUniforms(viewMatrix, projectionMatrix);
        p.onBeforeWorldRender(viewMatrix, projectionMatrix);
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Z"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
                "Lorg/joml/Matrix4f;"+
                "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "Z"+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void afterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        p.onAfterWorldRender();
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    void onRenderLevelEnd(
        CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f viewMatrix,
        @Local(ordinal = 1) Matrix4f projectionMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        p.onAfterRenderHand();

        Uniforms.FRX_LAST_VIEW_MATRIX.set(viewMatrix);
        Uniforms.FRX_LAST_PROJECTION_MATRIX.set(projectionMatrix);
        Uniforms.FRX_LAST_CAMERA_POS.set(this.mainCamera.position().toVector3f());
    }

    @ModifyExpressionValue(
        method = "resize",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget useOriginalMainRenderTargetOnResize(RenderTarget mainRenderTarget) {
        if (Pipelines.getCurrent() != null) {
            RenderTarget mainRenderTargetOverride = mainRenderTarget;
            ((MinecraftExtended) this.minecraft).canpipe_setMainRenderTargetOverride(null);
            mainRenderTarget = this.minecraft.getMainRenderTarget();
            ((MinecraftExtended) this.minecraft).canpipe_setMainRenderTargetOverride(mainRenderTargetOverride);
        }
        return mainRenderTarget;
    }

    @Override
    public ShadowFrustum[] canpipe_getShadowFrustums() {
        return this.canpipe_shadowFrustums;
    }

    public ChunkSectionsToRender[] canpipe_getChunkSectionsToRender() {
        return this.canpipe_chunkSectionsToRender;
    }

    @Override
    public FogRenderer canpipe_getFogRenderer() {
        return this.fogRenderer;
    }

    @Override
    public Matrix4f canpipe_worldViewMatrix() {
        return this.canpipe_worldViewMatrix;
    }

    @Override
    public Matrix4f canpipe_worldProjectionMatrix() {
        return this.canpipe_worldProjectionMatrix;
    }

    @Override
    public int canpipe_getRenderTarget() {
        return this.canpipe_renderTarget;
    }

    @Override
    public void canpipe_setRenderTarget(int renderTarget) {
        this.canpipe_renderTarget = renderTarget;
    }

}
