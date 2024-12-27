package fewizz.canpipe.compat.indigo.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

@Mixin(MutableQuadViewImpl.class)
public abstract class MutableQuadViewImplMixin extends QuadViewImplMixin implements MutableQuadViewExtended {

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    void onClear(CallbackInfo ci) {
        Arrays.fill(this.ao, 1.0F);
        this.spriteIndex = -1;
    }

    @Inject(
        method = "fromVanilla("+
            "Lnet/minecraft/client/renderer/block/model/BakedQuad;"+
            "Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;"+
            "Lnet/minecraft/core/Direction;"+
        ")Lnet/fabricmc/fabric/impl/client/indigo/renderer/mesh/MutableQuadViewImpl;",
        at = @At("TAIL")
    )
    private void onFromVanilla(BakedQuad quad, RenderMaterial mat, Direction d, CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        if (Pipelines.getCurrent() != null) {
            Arrays.fill(this.ao, 1.0F);
            this.spriteIndex = ((TextureAtlasSpriteExtended) quad.getSprite()).getIndex();
        }
    }

    @Override
    public void setSpriteIndex(int index) {
        this.spriteIndex = index;
    }

    @Override
    public void setAO(int index, float value) {
        this.ao[index] = value;
    }

}
