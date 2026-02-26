package fewizz.canpipe.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.systems.RenderSystem;

import fewizz.canpipe.mixininterface.CameraExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

@Mixin(Camera.class)
public class CameraMixin implements CameraExtended {

    @Shadow @Final private Minecraft minecraft = Minecraft.getInstance();
    @Shadow private float fov;

    @Override
    public Matrix4f canpipe_createProjectionMatrixForCulling(float depthFar) {
        float fovForCulling = Math.max(this.fov, this.minecraft.options.fov().get().intValue());
        Matrix4f projection = new Matrix4f();
        return projection.perspective(
            fovForCulling * (float) (Math.PI / 180.0),
            (float)this.minecraft.getWindow().getWidth() / this.minecraft.getWindow().getHeight(),
            0.05F,
            depthFar,
            RenderSystem.getDevice().isZZeroToOne()
        );
    }

}
