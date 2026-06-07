package fewizz.canpipe.compat.indigo.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fewizz.canpipe.compat.indigo.MutableQuadViewExtended;
import fewizz.canpipe.material.Material;
import fewizz.canpipe.pipeline.Pipelines;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;

@Mixin(MutableQuadViewImpl.class)
public abstract class MutableQuadViewImplMixin extends QuadViewImplMixin implements MutableQuadViewExtended {

    @Override
    public void canpipe_setSprite(TextureAtlasSprite sprite) {
        this.canpipe_atlasSprite = sprite;
    }

    @Override
    public void canpipe_setAO(int index, float value) {
        int i = (baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 2;
        this.canpipe_quadData[i] &= ~(0xFF << (index * 8));
        int valueI = (int) (Math.clamp(value, 0.0F, 1.0F) * 255.0F);
        this.canpipe_quadData[i] |= (valueI << (index * 8));
    }

    @Override
    public void canpipe_setMaterialSupplier(Function<TextureAtlasSprite, Material> materialSupplier) {
        this.canpipe_materialSupplier = materialSupplier;
    }

    @Inject(method = "clear", at = @At("TAIL"), remap = false)
    void onClear(CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        if (this.canpipe_quadData == null) {return;}

        int i = (baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 2;
        this.canpipe_quadData[i] |= 0xFFFFFFFF;
        this.canpipe_atlasSprite = null;
    }

    @Inject(method = "fromBakedQuad", at = @At("HEAD"))
    private void onFromBakedQuad(BakedQuad quad, CallbackInfoReturnable<MutableQuadViewImpl> ci) {
        if (this.canpipe_quadData == null) {return;}

        int i = (baseIndex / EncodingFormat.TOTAL_STRIDE) * CANPIPE_DATA_STRIDE_INTS + 2;
        this.canpipe_quadData[i] |= 0xFFFFFFFF;

        if (Pipelines.getCurrent() != null) {
            this.canpipe_atlasSprite = quad.materialInfo().sprite();
        }
    }

}
