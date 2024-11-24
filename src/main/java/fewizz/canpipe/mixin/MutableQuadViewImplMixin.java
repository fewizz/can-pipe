package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.mixininterface.MutableQuadViewImplExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.NormalHelper;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

@Mixin(value = MutableQuadViewImpl.class, remap = false)
public abstract class MutableQuadViewImplMixin implements MutableQuadViewImplExtended {

    private int spriteIndex;
    private int tangent;

    @Inject(method = "clear", at = @At("TAIL"))
    void onClear(CallbackInfo ci) {
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
        var mc = Minecraft.getInstance();
        var as = mc.getModelManager().getAtlas(quad.getSprite().atlasLocation());
        this.spriteIndex = ((TextureAtlasExtended) as).indexBySprite(quad.getSprite());
    }


    @Override
    public void setSpriteIndex(int index) {
        this.spriteIndex = index;
    }


    @Override
    public int getSpriteIndex() {
        return this.spriteIndex;
    }

    @Override
    public int getTangent() {
        return this.tangent;
    }

    @Override
    public void computeTangent() {
        // taken from frex
        final float v1 = this.u(0);
        final float dv0 = v1 - this.v(0);
        final float dv1 = this.v(2) - v1;
        final float u1 = this.u(1);
        final float du0 = u1 - this.u(0);
        final float du1 = this.u(2) - u1;
        final float inverseLength = 1.0f / (du0 * dv1 - du1 * dv0);

        final float x1 = x(1);
        final float y1 = y(1);
        final float z1 = z(1);

        final float tx = inverseLength * (dv1 * (x1 - x(0)) - dv0 * (x(2) - x1));
        final float ty = inverseLength * (dv1 * (y1 - y(0)) - dv0 * (y(2) - y1));
        final float tz = inverseLength * (dv1 * (z1 - z(0)) - dv0 * (z(2) - z1));

        // TODO
        // final float bx = inverseLength * (-du1 * (x1 - x(0)) + du0 * (x(2) - x1));
        // final float by = inverseLength * (-du1 * (y1 - y(0)) + du0 * (y(2) - y1));
        // final float bz = inverseLength * (-du1 * (z1 - z(0)) + du0 * (z(2) - z1));

        // Compute handedness
        // final float nx = this.normalX(0);
        // final float ny = this.normalY(0);
        // final float nz = this.normalZ(0);

        // T cross N
        // final float TcNx = ty * nz - tz * ny;
        // final float TcNy = tz * nx - tx * nz;
        // final float TcNz = tx * ny - ty * nx;

        // B dot TcN
        // final float BdotTcN = bx * TcNx + by * TcNy + bz * TcNz;
        // final boolean inverted = BdotTcN < 0f;

        this.tangent = NormalHelper.packNormal(tx, ty, tz);
    }

}
