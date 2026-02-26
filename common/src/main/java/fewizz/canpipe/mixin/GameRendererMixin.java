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

import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.Uniforms;
import fewizz.canpipe.mixininterface.CameraExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.Profiler;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererExtended {

    @Shadow @Final Minecraft minecraft;
    @Shadow @Final private Camera mainCamera;
    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private LevelRenderState levelRenderState;

    @Unique private long canpipe_renderStartNano = -1;
    @Unique private int canpipe_renderTarget = -1;
    @Unique private Matrix4f[] canpipe_shadowProjectionMatrices = null;
    @Unique private Matrix4f[] canpipe_shortendedViewProjectionMatrices = null;
    @Unique private Vector3f[] canpipe_shadowInnerOffsets = null;
    @Unique private Matrix4f canpipe_worldViewMatrix = null;
    @Unique private Matrix4f canpipe_worldProjectionMatrix = null;

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

        this.canpipe_shadowProjectionMatrices = new Matrix4f[] {
            new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
        };
        this.canpipe_shortendedViewProjectionMatrices = new Matrix4f[] {
            new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
        };
        this.canpipe_shadowInnerOffsets = new Vector3f[] {
            new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()
        };
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
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Z"+
                "Lnet/minecraft/client/renderer/state/CameraRenderState;"+
                "Lorg/joml/Matrix4f;"+
                "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "Z"+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"+
            ")V"
        )
    )
    void onBeforeWorldRender(
        DeltaTracker deltaTracker,
        CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f viewMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return;
        }

        Matrix4f projectionMatrix = this.levelRenderState.cameraRenderState.projectionMatrix;

        this.canpipe_worldViewMatrix = new Matrix4f(viewMatrix);
        this.canpipe_worldProjectionMatrix = new Matrix4f(projectionMatrix);

        Uniforms.CANPIPE_RENDER_FRAMES.add(1);
        Uniforms.FRX_RENDER_SECONDS.set((float)((System.nanoTime() - this.canpipe_renderStartNano) / 1_000_000_000.0));

        Uniforms.FRX_CAMERA_POS.set(this.mainCamera.position().toVector3f());

        if (Uniforms.FRX_LAST_VIEW_MATRIX.get(0, 0) == Float.NEGATIVE_INFINITY) {
            Uniforms.FRX_LAST_VIEW_MATRIX.set(viewMatrix);
            Uniforms.FRX_LAST_PROJECTION_MATRIX.set(projectionMatrix);
            Uniforms.FRX_LAST_CAMERA_POS.set(Uniforms.FRX_CAMERA_POS);
        }

        float renderDistance = this.minecraft.options.getEffectiveRenderDistance() * 16;

        if (p.shadows != null) {
            Profiler.get().push("can-pipe calculate shadow uniforms");

            Vector3f toSunDir = p.getSunOrMoonDir(this.minecraft.level, new Vector3f());
            Vector3f sunPosOffset = toSunDir.mul(renderDistance + 48, new Vector3f());

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

            final float maxCascadeRadius = renderDistance + 48;

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

                float depthTextureSize = (float) p.shadows.framebuffers().get(0).getDepthTexture().getWidth(0);
                float metersPerPixel = cascadeRadius*2.0F / depthTextureSize;

                Vector3f dPos = Uniforms.FRX_CAMERA_POS.sub(Uniforms.FRX_LAST_CAMERA_POS, new Vector3f());
                Vector3f dShadowPos = dPos.mul(shadowRotationMatrix).div(metersPerPixel);

                this.canpipe_shadowInnerOffsets[cascade].add(dShadowPos);
                this.canpipe_shadowInnerOffsets[cascade].sub(this.canpipe_shadowInnerOffsets[cascade].floor(new Vector3f()));

                              // for camera rotation                         // for position change
                center.x -= (center.x % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].x * metersPerPixel;
                center.y -= (center.y % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].y * metersPerPixel;
                center.z -= (center.z % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].z * metersPerPixel;

                Uniforms.CANPIPE_SHADOW_CENTERS[cascade].set(center.x, center.y, center.z, cascadeRadius);

                // sometimes cascade is out of frustum bounds
                // we don't want to render chunks and entiteis more than needed, right?
                // (help)
                float depthFar = Float.MAX_VALUE;

                for (int x = -1; x <= 1; x += 2) {
                    for (int y = -1; y <= 1; y += 2) {
                        for (int z = -1; z <= 1; z += 2) {
                            var edge = new Vector3f(
                                center.x + cascadeRadius*x,
                                center.y + cascadeRadius*y,
                                center.z + cascadeRadius*z
                            ).mulProject(inverseShadowViewMatrix).mulProject(viewMatrix);

                            depthFar = Math.min(
                                Math.max(depthFar, -edge.z),
                                renderDistance + 48.0F
                            );
                        }
                    }
                }

                this.canpipe_shortendedViewProjectionMatrices[cascade] =
                    ((CameraExtended) this.mainCamera).canpipe_createProjectionMatrixForCulling(depthFar)
                    .mul(viewMatrix);

                Vector3f min = new Vector3f();
                Vector3f max = new Vector3f();

                new Matrix4f(Uniforms.FRX_SHADOW_VIEW_MATRIX).mul(
                    ((CameraExtended) this.mainCamera).canpipe_createProjectionMatrixForCulling(depthFar)
                    .mul(viewMatrix).invert()
                ).frustumAabb(min, max);  // frustum AABB in shadow view space

                // those matrices aren't passed into shadow material programs,
                // no need to worry about constant radius
                this.canpipe_shadowProjectionMatrices[cascade].setOrtho(
                    Math.max(min.x, center.x - cascadeRadius),  // left
                    Math.min(max.x, center.x + cascadeRadius),  // right
                    Math.max(min.y, center.y - cascadeRadius),  // bottom
                    Math.min(max.y, center.y + cascadeRadius),  // up
                    0.0F,                       // near
                   -Math.max(min.z, center.z - cascadeRadius)   // far
                );
            }
            Profiler.get().pop();
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
                "Lnet/minecraft/client/renderer/state/CameraRenderState;"+
                "Lorg/joml/Matrix4f;"+
                "Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "Z"+
                "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void onAfterLevelRender(DeltaTracker deltaTracker, CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            p.onAfterWorldRender();
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    void onRenderLevelEnd(
        CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f projectionMatrix,
        @Local(ordinal = 1) Matrix4f viewMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            p.onAfterRenderHand();
        }

        Uniforms.FRX_LAST_VIEW_MATRIX.set(viewMatrix);
        Uniforms.FRX_LAST_PROJECTION_MATRIX.set(projectionMatrix);
        Uniforms.FRX_LAST_CAMERA_POS.set(this.mainCamera.position().toVector3f());
    }

    @Override
    public Matrix4f[] canpipe_getShadowProjectionMatrices() {
        return this.canpipe_shadowProjectionMatrices;
    }

    @Override
    public Matrix4f[] canpipe_getShortenedViewProjectionMatrices() {
        return this.canpipe_shortendedViewProjectionMatrices;
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
