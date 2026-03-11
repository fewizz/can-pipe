package fewizz.canpipe.compat.indigo.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@Mixin(MutableQuadViewImpl.class)
public abstract class MutableQuadViewImplMixin extends QuadViewImplMixin implements MutableQuadViewExtended {

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    void onClear(CallbackInfo ci) {
        Arrays.fill(this.ao, 1.0F);
        this.sprite = null;
    }

    @Inject(method = "fromBakedQuad", at = @At("HEAD"))
    private void onFromBakedQuad(BakedQuad quad, CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        if (Pipelines.getCurrent() != null) {
            Arrays.fill(this.ao, 1.0F);
            this.sprite = quad.materialInfo().sprite();
        }
    }

    @Override
    public void canpipe_setSprite(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void canpipe_setAO(int index, float value) {
        this.ao[index] = value;
    }

}
