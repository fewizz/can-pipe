package fewizz.canpipe.compat.indigo.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@Mixin(MutableQuadViewImpl.class)
public abstract class MutableQuadViewImplMixin extends QuadViewImplMixin implements MutableQuadViewExtended {

    @Override
    public void canpipe_setSprite(TextureAtlasSprite sprite) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        this.canpipe_extraData[i+0] = ((TextureAtlasSpriteExtended) sprite).canpipe_getIndex();
    }

    @Override
    public void canpipe_setAO(int index, float value) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        this.canpipe_extraData[i+2] &= ~(0xFF << (index * 8));
        int valueI = (int) (Math.clamp(value, 0.0F, 1.0F) * 255.0F);
        this.canpipe_extraData[i+2] |= (valueI << (index * 8));
    }

    @Override
    public void canpipe_setMaterialSupplier(Function<TextureAtlasSprite, Material> materialSupplier) {
        this.canpipe_materialSupplier = materialSupplier;
    }

    @Inject(method = "uv", at = @At("RETURN"))
    void onUV(CallbackInfoReturnable<MutableQuadViewImpl> cir) {
        if (this.canpipe_materialSupplier == null) { return; }

        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;

        int spriteIndex = this.canpipe_extraData[i+0];
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(this.atlas().getId());
        TextureAtlasSprite sprite = ((TextureAtlasExtended) atlas).canpipe_getSpriteById(spriteIndex);

        Material material = this.canpipe_materialSupplier.apply(sprite);

        if (material != null) {
            this.canpipe_extraData[i+1] = material.id();
        }
    }

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    void onClear(CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        this.canpipe_extraData[i+0] |= 0xFFFFFFFF;
        this.canpipe_extraData[i+1] |= 0xFFFFFFFF;
        this.canpipe_extraData[i+2] |= 0xFFFFFFFF;
    }

    @Inject(method = "fromBakedQuad", at = @At("HEAD"))
    private void onFromBakedQuad(BakedQuad quad, CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        int i = this.baseIndex / EncodingFormat.TOTAL_STRIDE * CANPIPE_DATA_STRIDE_INTS;
        this.canpipe_extraData[i+0] = ((TextureAtlasSpriteExtended) quad.materialInfo().sprite()).canpipe_getIndex();
        this.canpipe_extraData[i+2] |= 0xFFFFFFFF;
    }

}
