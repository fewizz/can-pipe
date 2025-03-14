package fewizz.canpipe.mixin;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import fewizz.canpipe.mixininterface.GameRendererAccessor;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererAccessor {

    @Shadow @Final Minecraft minecraft;
    @Shadow @Final private Camera mainCamera;
    @Shadow private float renderDistance;
    @Shadow private float fovModifier;

    @Shadow public Matrix4f getProjectionMatrix(float fov) { return null; }

    @Unique private int canpipe_frame = -1;
    @Unique private long canpipe_renderStartNano = -1;
    @Unique private long canpipe_renderNanos = -1;
    @Unique private Vector3f canpipe_cameraPos = null;
    @Unique private Vector3f canpipe_lastCameraPos = null;
    @Unique private Matrix4f canpipe_projectionMatrix = null;
    @Unique private Matrix4f canpipe_lastProjectionMatrix = null;
    @Unique private Matrix4f canpipe_viewMatrix = null;
    @Unique private Matrix4f canpipe_lastViewMatrix = null;
    @Unique private Matrix4f canpipe_shadowViewMatrix = null;
    @Unique private Matrix4f[] canpipe_shadowProjectionMatrices = null;
    @Unique private Vector3f[] canpipe_shadowInnerOffsets = null;
    @Unique private Vector4f[] canpipe_shadowCenters = null;
    @Unique private Float canpipe_depthFarOverride = null;

    @Override
    public int canpipe_getFrame() {
        return canpipe_frame;
    }

    @Override
    public void canpipe_onPipelineActivated() {
        this.canpipe_renderStartNano = System.nanoTime();
        this.canpipe_renderNanos = -1;
        this.canpipe_frame = -1;

        // not sure why, but i can't initialize them above (mixin bug?)
        this.canpipe_viewMatrix = new Matrix4f();
        this.canpipe_lastViewMatrix = null;

        this.canpipe_projectionMatrix = new Matrix4f();
        this.canpipe_lastProjectionMatrix = null;

        this.canpipe_cameraPos = new Vector3f();
        this.canpipe_lastCameraPos = null;

        this.canpipe_shadowViewMatrix = new Matrix4f();

        this.canpipe_shadowProjectionMatrices = new Matrix4f[] {
            new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
        };
        this.canpipe_shadowInnerOffsets = new Vector3f[] {
            new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()
        };
        this.canpipe_shadowCenters = new Vector4f[] {
            new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()
        };
    }

    @Override
    public float canpipe_getRenderSeconds() {
        return (float) (this.canpipe_renderNanos / 1000000000.0);
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
                "Lnet/minecraft/client/Camera;"+
                "Lnet/minecraft/client/renderer/GameRenderer;"+
                "Lorg/joml/Matrix4f;"+
                "Lorg/joml/Matrix4f;"+
            ")V"
        )
    )
    void onBeforeWorldRender(
        DeltaTracker deltaTracker,
        CallbackInfo ci,
        @Local(ordinal = 0) Matrix4f projectionMatrix,
        @Local(ordinal = 2) Matrix4f viewMatrix
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) {
            return;
        }

        float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
        this.canpipe_frame += 1;
        this.canpipe_renderNanos = System.nanoTime() - this.canpipe_renderStartNano;

        if (this.canpipe_lastViewMatrix == null) {
            this.canpipe_lastViewMatrix = new Matrix4f(viewMatrix);
            this.canpipe_lastProjectionMatrix = new Matrix4f(projectionMatrix);
            this.canpipe_lastCameraPos = new Vector3f(this.mainCamera.getPosition().toVector3f());
        }
        else {
            this.canpipe_lastViewMatrix.set(this.canpipe_viewMatrix);
            this.canpipe_lastProjectionMatrix.set(this.canpipe_projectionMatrix);
            this.canpipe_lastCameraPos.set(this.canpipe_cameraPos);
        }

        this.canpipe_viewMatrix.set(viewMatrix);
        this.canpipe_projectionMatrix.set(projectionMatrix);
        this.canpipe_cameraPos.set(this.mainCamera.getPosition().toVector3f());

        if (p.skyShadows != null) {
            Vector3f toSunDir = p.getSunOrMoonDir(this.minecraft.level, new Vector3f(), pt);
            Vector3f sunPosOffset = toSunDir.mul(this.renderDistance + 48, new Vector3f());

            this.canpipe_shadowViewMatrix.setLookAt(
                sunPosOffset,                                  // eye pos
                new Vector3f(0.0F, 0.0F, 0.0F),                // center
                !(sunPosOffset.x == 0 && sunPosOffset.z == 0)  // up
                    ? new Vector3f(0.0F, 1.0F, 0.0F)
                    : new Vector3f(0.0F, 0.0F, 1.0F)
            );
            var shadowRotationMatrix = new Matrix3f(this.canpipe_shadowViewMatrix);

            this.canpipe_depthFarOverride = this.renderDistance + 48.0F;
                float fov = (float) this.minecraft.options.fov().get().intValue();
                var pm = this.getProjectionMatrix(fov);
                Frustum cascadeFrustum = new Frustum(viewMatrix, pm);
                this.canpipe_depthFarOverride = null;

            for (int cascade = 0; cascade < 4; ++cascade) {
                float cascadeRadius;
                Vector4f center;

                if (cascade == 0) {
                    // TODO this can be more efficient
                    cascadeRadius = (this.renderDistance + 48.0F);
                    center = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
                }
                else {
                    cascadeRadius = p.skyShadows.cascadeRadii().get(cascade-1);
                    center = new Vector4f(
                        mainCamera.getLookVector().mul(cascadeRadius, new Vector3f()),
                        1.0F
                    );
                }

                center.mul(canpipe_shadowViewMatrix);
                center.div(center.w);

                float depthTextureSize = (float) p.skyShadows.framebuffer().depthAttachment.texture().extent.x;
                float metersPerPixel = cascadeRadius*2.0F / depthTextureSize;

                Vector3f dPos = this.canpipe_cameraPos.sub(this.canpipe_lastCameraPos, new Vector3f());
                Vector3f dShadowPos = dPos.mul(shadowRotationMatrix).div(metersPerPixel);

                this.canpipe_shadowInnerOffsets[cascade].add(dShadowPos);
                this.canpipe_shadowInnerOffsets[cascade].sub(this.canpipe_shadowInnerOffsets[cascade].floor(new Vector3f()));

                              // for camera rotation                         // for position change
                center.x -= (center.x % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].x * metersPerPixel;
                center.y -= (center.y % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].y * metersPerPixel;
                center.z -= (center.z % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].z * metersPerPixel;

                this.canpipe_shadowCenters[cascade].set(center.x, center.y, center.z, cascadeRadius);

                // sometimes cascade is out of frustum bounds
                // we don't want to render chunks and entiteis more than needed, right? (help)
                Vector3f min = new Vector3f(+Float.MAX_VALUE);
                Vector3f max = new Vector3f(-Float.MAX_VALUE);

                // could be more efficient
                for (Vector4f point : cascadeFrustum.getFrustumPoints()) {
                    point.mul(this.canpipe_shadowViewMatrix);
                    point.div(point.w);
                    min.min(point.xyz(new Vector3f()));
                    max.max(point.xyz(new Vector3f()));
                }

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
        }

        p.onBeforeWorldRender(this.canpipe_viewMatrix, this.canpipe_projectionMatrix);
        this.minecraft.mainRenderTarget.bindWrite(true);
    }

    @ModifyArg(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;getFov(Lnet/minecraft/client/Camera;FZ)F"
        ),
        index = 2
    )
    private boolean fixZeroFovOnFirstFrame(boolean useFovSetting) {
        if (this.fovModifier == 0.0) {
            return false;
        }
        return useFovSetting;
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Z"+
                "Lnet/minecraft/client/Camera;"+
                "Lnet/minecraft/client/renderer/GameRenderer;"+
                "Lorg/joml/Matrix4f;"+
                "Lorg/joml/Matrix4f;"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void onAfterLevelRender(
        DeltaTracker deltaTracker,
        CallbackInfo ci
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            p.onAfterWorldRender(canpipe_viewMatrix, canpipe_projectionMatrix);
            this.minecraft.mainRenderTarget.bindWrite(false);
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    void onRenderLevelEnd(CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            p.onAfterRenderHand(canpipe_viewMatrix, canpipe_projectionMatrix);
        }
    }

    @WrapMethod(method = "getDepthFar")
    float wrapGetDepthFar(Operation<Float> original) {
        if (this.canpipe_depthFarOverride != null) {
            return this.canpipe_depthFarOverride;
        }
        return original.call();
    }

    @Override
    public Vector3f canpipe_getLastCameraPos() {
        return this.canpipe_lastCameraPos;
    }

    @Override
    public Matrix4f canpipe_getLastViewMatrix() {
        return this.canpipe_lastViewMatrix;
    }

    @Override
    public Matrix4f canpipe_getLastProjectionMatrix() {
        return this.canpipe_lastProjectionMatrix;
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

}
