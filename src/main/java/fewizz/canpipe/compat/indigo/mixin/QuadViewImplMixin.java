package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.compat.indigo.mixininterface.QuadViewExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;

@Mixin(value = QuadViewImpl.class, remap = false)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    protected final float[] ao = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    protected int spriteIndex;

    @Override
    public int getSpriteIndex() {
        return this.spriteIndex;
    }

    @Override
    public float getAO(int index) {
        return this.ao[index];
    }

    @ModifyReturnValue(method = "hasShade", at = @At("RETURN"))
    boolean hasShade(boolean original) {
        // diffuse lighting is handled by pipeline
        if (Pipelines.getCurrent() != null) {
            return false;
        }
        return original;
    }

}
