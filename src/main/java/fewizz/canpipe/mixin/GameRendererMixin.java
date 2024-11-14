package fewizz.canpipe.mixin;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;

import fewizz.canpipe.Mod;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererAccessor {

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    private int canpipe_frame = -1;
    private Vector3f canpipe_lastCameraPos = new Vector3f();
    private Matrix4f canpipe_projection = new Matrix4f();
    private Matrix4f canpipe_lastProjection = new Matrix4f();
    private Matrix4f canpipe_view = new Matrix4f();
    private Matrix4f canpipe_lastView = new Matrix4f();

    @Override
    public int canpipe_getFrame() {
        return canpipe_frame;
    }

    @Inject(method = "render", at=@At("HEAD"))
    void onBeforeRender(CallbackInfo ci) {
        canpipe_frame += 1;
    }

    @Inject(method = "resize", at = @At("HEAD"))
    void onResize(int w, int h, CallbackInfo ci) {
        Mod.onGameRendererResize(w, h);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    void onBeforeRenderLevel(CallbackInfo ci) {
        canpipe_lastCameraPos = this.mainCamera.getPosition().toVector3f();
    }

    @WrapOperation(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
                "Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;"+
                "Lnet/minecraft/client/DeltaTracker;"+
                "Z"+
                "Lnet/minecraft/client/Camera;"+
                "Lnet/minecraft/client/renderer/GameRenderer;"+
                "Lnet/minecraft/client/renderer/LightTexture;"+
                "Lorg/joml/Matrix4f;"+
                "Lorg/joml/Matrix4f;"+
            ")V"
        )
    )
    void levelRendererRenderLevelWrapper(
        LevelRenderer instance,
        GraphicsResourceAllocator resourcePool,
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f view,
        Matrix4f projection,
        Operation<Void> original
    ) {
        canpipe_lastView = canpipe_view;
        canpipe_lastProjection = canpipe_projection;

        canpipe_view = view;
        canpipe_projection = projection;

        Mod.onBeforeWorldRender(view, projection);
        this.minecraft.mainRenderTarget.bindWrite(false);

        original.call(instance, resourcePool, deltaTracker, bl, camera, gameRenderer, lightTexture, view, projection);

        Mod.onAfterWorldRender(view, projection);
        this.minecraft.mainRenderTarget.bindWrite(false);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    void onAfterRenderLevel(CallbackInfo ci) {
        Mod.onAfterRenderHand(canpipe_view, canpipe_projection);
    }

    @Override
    public Vector3f canpipe_getLastCameraPos() {
        return this.canpipe_lastCameraPos;
    }

    @Override
    public Matrix4f canpipe_getLastViewMatrix() {
        return this.canpipe_lastView;
    }

    @Override
    public Matrix4f canpipe_getLastProjectionMatrix() {
        return this.canpipe_lastProjection;
    }

}
