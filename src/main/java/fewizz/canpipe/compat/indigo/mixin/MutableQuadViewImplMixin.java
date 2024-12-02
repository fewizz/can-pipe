package fewizz.canpipe.compat.indigo.mixin;

import java.util.Arrays;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.Pipelines;
import fewizz.canpipe.compat.indigo.mixininterface.MutableQuadViewExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.NormalHelper;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

@Mixin(value = MutableQuadViewImpl.class, remap = false)
public abstract class MutableQuadViewImplMixin extends QuadViewImplMixin implements MutableQuadViewExtended {

    @Inject(method = "clear", at = @At("TAIL"))
    void onClear(CallbackInfo ci) {
        Arrays.fill(this.ao, 1.0F);
        this.spriteIndex = -1;
        this.tangent = 0x00000000;
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

            var mc = Minecraft.getInstance();
            var as = mc.getModelManager().getAtlas(quad.getSprite().atlasLocation());
            this.spriteIndex = ((TextureAtlasExtended) as).indexBySprite(quad.getSprite());
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

    @Override
    public void computeTangent() {
        Vector3f t = Pipelines.computeTangent(
            x(0), y(0), z(0), u(0), v(0),
            x(1), y(1), z(1), u(1), v(1),
            x(2), y(2), z(2), u(2), v(2)
        );
        this.tangent = NormalHelper.packNormal(t.x, t.y, t.z);
    }

}
