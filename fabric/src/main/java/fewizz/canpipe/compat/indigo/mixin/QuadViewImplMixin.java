package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.compat.indigo.mixininterface.QuadViewExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(value = QuadViewImpl.class, remap = false)
public abstract class QuadViewImplMixin implements QuadViewExtended {

    @Unique protected final float[] ao = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    @Unique protected TextureAtlasSprite sprite;

    @Override
    public TextureAtlasSprite canpipe_getSprite() {
        return this.sprite;
    }

    @Override
    public float canpipe_getAO(int index) {
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
