package fewizz.canpipe.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixininterface.CameraExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

@Mixin(Camera.class)
public class CameraMixin implements CameraExtended {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private float fov;
    @Shadow private float fovModifier;

    @Shadow private void tickFov() {}

    @Inject(method = "update", at = @At("RETURN"))
    void onUpdate(CallbackInfo ci) {
        // on first frame `this.calculateFov()` will return 0.0F,
        // because `this.tickFov()` wasn't called and this.fovModifier is 0.0F
        // That will lead to invalid projection matrix
        if (this.fovModifier == 0.0F) {
            this.tickFov();
        }
    }

    @Override
    public Matrix4f canpipe_createProjectionMatrixForCulling(float depthFar) {
        float fovForCulling = Math.max(this.fov, this.minecraft.options.fov().get().intValue());
        Matrix4f projection = new Matrix4f();
        return projection.perspective(
            fovForCulling * (float) (Math.PI / 180.0),
            (float) this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
            0.05F,
            depthFar,
            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne()
        );
    }

}
