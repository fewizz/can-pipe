package fewizz.canpipe.compat.cinnabar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import fewizz.canpipe.b3d.GpuTextureExtended;
import graphics.cinnabar.core.hg3d.Hg3DGpuTexture;

@Mixin(Hg3DGpuTexture.class)
public class Hg3DGpuTextureMixin implements GpuTextureExtended {

    @Unique protected FilterMode canpipe_mipFilter = null;
    @Unique protected AddressMode canpipe_addressModeW = null;
    @Unique protected DepthTestFunction canpipe_compareOp = null;

    @Override
    public void canpipe_setMipmapMode(FilterMode filterMode) {
        this.canpipe_mipFilter = filterMode;
    }

    @Override
    public void canpipe_setAddressModeW(AddressMode addressMode) {
        this.canpipe_addressModeW = addressMode;
    }

    @Override
    public void canpipe_setCompareOp(DepthTestFunction compareOp) {
        this.canpipe_compareOp = compareOp;
    }
    
}
