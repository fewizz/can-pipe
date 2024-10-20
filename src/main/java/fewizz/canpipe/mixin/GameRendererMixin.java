package fewizz.canpipe.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.Mod;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererAccessor {

    @Shadow
    @Final
    Minecraft minecraft;

    private int canpipe_frame = -1;
    private Matrix4f canpipe_projection = new Matrix4f();
    private Matrix4f canpipe_view = new Matrix4f();;

    @Override
    public int canpipe_getFrame() {
        return canpipe_frame;
    }

    @WrapOperation(
        method = "reloadShaders", 
        at = @At(value = "NEW", target = "Lnet/minecraft/client/renderer/ShaderInstance;")
    )
    ShaderInstance onReloadShaderMaterialProgramCreation(
        ResourceProvider resourceProvider,
        String name,
        VertexFormat vertexFormat,
        Operation<ShaderInstance> original
    ) {
        ShaderInstance result = Mod.tryGetMaterialProgramReplacement(name);
        if (result == null) {
            result = original.call(resourceProvider, name, vertexFormat);
        }
        return result;
    }

    @Inject(method = "render", at=@At("HEAD"))
    void onBeforeRender(CallbackInfo ci) {
        canpipe_frame += 1;
    }

    @Inject(method = "resize", at = @At("HEAD"))
    void onResize(int w, int h, CallbackInfo ci) {
        Mod.onGameRendererResize(w, h);
    }

    @WrapOperation(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("+
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
        DeltaTracker deltaTracker,
        boolean bl,
        Camera camera,
        GameRenderer gameRenderer,
        LightTexture lightTexture,
        Matrix4f view,
        Matrix4f projection,
        Operation<Void> original
    ) {
        canpipe_view = view;
        canpipe_projection = projection;

        Mod.onBeforeWorldRender(view, projection);
        this.minecraft.mainRenderTarget.bindWrite(false);

        original.call(instance, deltaTracker, bl, camera, gameRenderer, lightTexture, view, projection);

        Mod.onAfterWorldRender(view, projection);
        this.minecraft.mainRenderTarget.bindWrite(false);
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    void onAfterRenderLevel(CallbackInfo ci) {
        Mod.onAfterRenderHand(canpipe_view, canpipe_projection);
    }

}
