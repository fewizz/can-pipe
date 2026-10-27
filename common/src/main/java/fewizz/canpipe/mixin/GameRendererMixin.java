package fewizz.canpipe.mixin;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import fewizz.canpipe.mixininterface.CameraExtended;
import fewizz.canpipe.mixininterface.GameRendererExtended;
import fewizz.canpipe.pipeline.Framebuffer;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.Pipelines;
import fewizz.canpipe.pipeline.Uniforms;
import fewizz.canpipe.util.ShadowFrustum;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererExtended {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private Camera mainCamera;
    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private Lightmap lightmap;
    @Shadow @Final private RenderTarget mainRenderTarget;

    @Unique private long canpipe_renderStartNano = -1;
    @Unique private int canpipe_renderFrames = -1;
    @Unique private int canpipe_originType = 0;
    @Unique private boolean canpipe_isRenderingHand = false;
    @Unique private Vector3f canpipe_lastCameraPos = null;
    @Unique private Matrix4f canpipe_viewMatrix = null;
    @Unique private Matrix4f canpipe_projectionMatrix = null;
    @Unique private Matrix4f canpipe_shadowViewMatrix = null;
    @Unique private Matrix4f canpipe_lastViewMatrix;
    @Unique private Matrix4f canpipe_lastProjectionMatrix;
    @Unique private boolean canpipe_runResizePasses = false;
    @Unique private boolean canpipe_runInitPasses = false;
    @Unique private RenderTarget canpipe_mainRenderTargetOverride;

    @Unique private Vector3f[] canpipe_shadowInnerOffsets = null;
    @Unique private ShadowFrustum[] canpipe_shadowFrustums = null;
    @Unique private Vector4f[] canpipe_shadowCenters = null;

    @Override public FogRenderer canpipe_getFogRenderer() { return this.fogRenderer; }
    @Override public Matrix4f canpipe_worldViewMatrix() { return this.canpipe_viewMatrix; }
    @Override public Matrix4f canpipe_worldProjectionMatrix() { return this.canpipe_projectionMatrix; }
    @Override public boolean canpipe_isRenderingHand() { return this.canpipe_isRenderingHand; }
    @Override public Lightmap canpipe_getLightmap() { return this.lightmap; }
    @Override public int canpipe_getOriginType() { return this.canpipe_originType; }
    @Override public void canpipe_setOriginType(int originType) { this.canpipe_originType = originType; }
    @Override public void canpipe_setMainRenderTargetOverride(RenderTarget renderTarget) { this.canpipe_mainRenderTargetOverride = renderTarget; }

    @Override public ShadowFrustum[] canpipe_getShadowFrustums() { return this.canpipe_shadowFrustums; }
    @Override public Vector4f[] canpipe_getShadowCenters() { return this.canpipe_shadowCenters; }

    @Override
    public void canpipe_onPipelineActivated() {
        this.canpipe_lastCameraPos = new Vector3f(Float.NEGATIVE_INFINITY);
        this.canpipe_renderStartNano = System.nanoTime();
        this.canpipe_originType = 0;
        this.canpipe_isRenderingHand = false;

        this.canpipe_renderFrames = -1;

        this.canpipe_lastViewMatrix = new Matrix4f().m00(Float.NEGATIVE_INFINITY);
        this.canpipe_lastProjectionMatrix = new Matrix4f().m00(Float.NEGATIVE_INFINITY);

        this.canpipe_shadowViewMatrix = new Matrix4f();
        this.canpipe_shadowInnerOffsets = new Vector3f[] {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        this.canpipe_shadowCenters = new Vector4f[] {new Vector4f(), new Vector4f(), new Vector4f(), new Vector4f()};
        this.canpipe_shadowFrustums = new ShadowFrustum[4];

        this.canpipe_viewMatrix = null;
        this.canpipe_projectionMatrix = null;

        this.canpipe_runResizePasses = true;
        this.canpipe_runInitPasses = true;
    }

    @Inject(method = "resize", at = @At("HEAD"))
    void onResize(CallbackInfo ci) {
        Pipeline p = Pipelines.getCurrent();
        if (p != null) {
            p.onWindowSizeChanged();
            this.canpipe_runResizePasses = true;
        }
    }

    @Inject(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extract("+
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

        this.canpipe_renderFrames += 1;

        if (this.canpipe_lastCameraPos.get(0) == Float.NEGATIVE_INFINITY) {
            this.canpipe_lastCameraPos.set(this.mainCamera.position().toVector3f());
        }

        if (p.shadows == null) { return; }

        profiler.push("can-pipe calculate shadow frustums");

        final float maxCascadeRadius = this.minecraft.options.getEffectiveRenderDistance() * 16 + 48.0F;
        final float depthTextureSize = (float) p.shadows.framebuffers().get(0).getDepthTexture().getWidth(0);

        Vector3f toSunDir = p.getSunOrMoonDir(this.minecraft.level, new Vector3f());
        Vector3f sunPosOffset = toSunDir.mul(maxCascadeRadius, new Vector3f());

        this.canpipe_shadowViewMatrix.setLookAt(
            sunPosOffset,                                  // eye pos
            new Vector3f(0.0F, 0.0F, 0.0F),                // center
            !(sunPosOffset.x == 0 && sunPosOffset.z == 0)  // up
                ? new Vector3f(0.0F, 1.0F, 0.0F)
                : new Vector3f(0.0F, 0.0F, 1.0F)
        );

        var shadowRotationMatrix = new Matrix3f(this.canpipe_shadowViewMatrix);
        var inverseShadowViewMatrix = new Matrix4f(this.canpipe_shadowViewMatrix).invert();

        // from smallest to biggest
        for (int cascade = p.shadows.cascadeRadii().size(); cascade >= 0; --cascade) {
            float cascadeRadius;
            Vector3f center;

            if (cascade == 0) {  // biggest, radius depends on render distance
                cascadeRadius = maxCascadeRadius;
            }
            else {
                cascadeRadius = Math.min(maxCascadeRadius, p.shadows.cascadeRadii().get(cascade-1));
            }

            center = new Vector3f(mainCamera.forwardVector()).mul(cascadeRadius);
            center.mulProject(this.canpipe_shadowViewMatrix);

            final float metersPerPixel = cascadeRadius*2.0F / depthTextureSize;

            Vector3f dPos = this.mainCamera.position().toVector3f().sub(this.canpipe_lastCameraPos);
            Vector3f dShadowPos = dPos.mul(shadowRotationMatrix).div(metersPerPixel);

            this.canpipe_shadowInnerOffsets[cascade].add(dShadowPos);
            this.canpipe_shadowInnerOffsets[cascade].sub(this.canpipe_shadowInnerOffsets[cascade].floor(new Vector3f()));

                            // for camera rotation                         // for position change
            center.x -= (center.x % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].x * metersPerPixel;
            center.y -= (center.y % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].y * metersPerPixel;
            center.z -= (center.z % metersPerPixel) + this.canpipe_shadowInnerOffsets[cascade].z * metersPerPixel;

            this.canpipe_shadowCenters[cascade].set(center.x, center.y, center.z, cascadeRadius);

            // For shortened projection matrix, from player's perspective
            // Such frustum should include whole cascade along -z
            float depthFar = 0.0F;
            for (int x = -1; x <= 1; x += 2) {  // for each cascade corner
                for (int y = -1; y <= 1; y += 2) {
                    for (int z = -1; z <= 1; z += 2) {
                        var corner =
                            new Vector3f(x, y, z).mul(cascadeRadius).add(center)
                            .mulProject(inverseShadowViewMatrix).mulProject(viewMatrix);
                        depthFar = Math.min(Math.max(depthFar, -corner.z), maxCascadeRadius);
                    }
                }
            }

            var shortenedProjectionMatrix = ((CameraExtended) this.mainCamera).canpipe_createProjectionMatrixForCulling(depthFar, false);

            var shortendedViewProjectionMatrix = new Matrix4f(shortenedProjectionMatrix).mul(viewMatrix);

            Vector3f min = new Vector3f();
            Vector3f max = new Vector3f();

            new Matrix4f()
                .mul(this.canpipe_shadowViewMatrix)
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
                this.canpipe_shadowViewMatrix, shadowProjectionMatrix,
                shortendedViewProjectionMatrix, toSunDir
            );
            shadowFrustum.prepare(this.mainCamera.position().x, this.mainCamera.position().y, this.mainCamera.position().z);
            this.canpipe_shadowFrustums[cascade] = shadowFrustum;
        }

        profiler.pop();
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target =
            "Lnet/minecraft/client/renderer/LevelRenderer;render("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Z"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
                "Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "ZZ"+
            ")V"
        )
    )
    void beforeRenderLevel(
        CallbackInfo ci,
        @Local(name = "cameraState") CameraRenderState cameraState,
        @Local(name = "projectionMatrix") Matrix4f projectionMatrix  // with bob
    ) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        Matrix4f viewMatrix = cameraState.viewRotationMatrix;

        this.canpipe_viewMatrix = new Matrix4f(viewMatrix);
        this.canpipe_projectionMatrix = new Matrix4f(projectionMatrix);

        if (this.canpipe_lastViewMatrix.get(0, 0) == Float.NEGATIVE_INFINITY) {
            this.canpipe_lastViewMatrix.set(viewMatrix);
            this.canpipe_lastProjectionMatrix.set(projectionMatrix);
        }

        float renderSeconds = (float)((System.nanoTime() - this.canpipe_renderStartNano) / 1_000_000_000.0);

        Uniforms.update(
            viewMatrix, projectionMatrix,
            this.canpipe_lastViewMatrix, this.canpipe_lastProjectionMatrix,
            this.canpipe_renderFrames, renderSeconds,
            this.canpipe_lastCameraPos,
            this.canpipe_shadowViewMatrix, this.canpipe_shadowCenters
        );

        this.canpipe_originType = 0;  // camera

        p.onBeforeRenderingLevel(this.canpipe_runResizePasses, this.canpipe_runInitPasses);

        this.canpipe_runInitPasses = false;
        this.canpipe_runResizePasses = false;
    }

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;render("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Z"+
                "Lnet/minecraft/client/renderer/state/level/CameraRenderState;"+
                "Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"+
                "Lorg/joml/Vector4f;"+
                "ZZ"+
            ")V",
            shift = Shift.AFTER
        )
    )
    void afterRenderLevel(CallbackInfo ci, @Local ProfilerFiller profiler) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        this.canpipe_isRenderingHand = true;

        profiler.popPush("can-pipe fabulous passes");
        this.canpipe_originType = 2;  // camera
        p.onAfterWorldRender();
    }

    @Inject(
        method = "render3dHud",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand", shift = Shift.AFTER)
    )
    void onRenderLevelEnd(CallbackInfo ci, @Local ProfilerFiller profiler) {
        Pipeline p = Pipelines.getCurrent();
        if (p == null) { return; }

        profiler.popPush("can-pipe after redner hand");

        this.canpipe_isRenderingHand = false;
        p.onAfterRenderHand();

        this.canpipe_lastViewMatrix.set(this.canpipe_viewMatrix);
        this.canpipe_lastProjectionMatrix.set(this.canpipe_projectionMatrix);
        this.canpipe_lastCameraPos = this.mainCamera.position().toVector3f();
    }

    @ModifyExpressionValue(
        method = {"mainRenderTarget", "processBlurEffect", "render", "renderLevel", "takeAutoScreenshot", "renderItemInHand", "render3dHud"},
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;"
        )
    )
    RenderTarget replaceMainRenderTarget(RenderTarget renderTarget) {
        if (this.canpipe_mainRenderTargetOverride != null) {
            renderTarget = this.canpipe_mainRenderTargetOverride;
        }
        return renderTarget;
    }

    @ModifyReturnValue(
        method = "useImprovedTransparency",
        at = @At("RETURN")
    )
    boolean useImprovedTransparency(boolean value) {
        return value && Pipelines.getCurrent() == null;
    }

    @WrapOperation(
        method = "renderItemInHand",
        at = @At(value = "INVOKE", target = "Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass")
    )
    RenderPass replaceItemInHandRenderPass(
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
            final Framebuffer fb = p.solidFramebuffer;

            RenderPassDescriptor.Builder descriptor = RenderPassDescriptor.builder(label);
            for (var colorAttachment : fb.colorTextureViews) { descriptor.withColorAttachment(colorAttachment); }
            descriptor.withDepthAttachment(fb.getDepthTextureView());
            descriptor.withRenderArea(new RenderPass.RenderArea(0, 0, fb.width, fb.height));

            return commandEncoder.createRenderPass(descriptor.build());
        }

        return operation.call(commandEncoder, label, colorTexture, clearColor, depthTexture, clearDepth);
    }

}