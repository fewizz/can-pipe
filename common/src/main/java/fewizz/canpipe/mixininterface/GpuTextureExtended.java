package fewizz.canpipe.mixininterface;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

import fewizz.canpipe.TextureCompareOp;
import fewizz.canpipe.TextureType;

public interface GpuTextureExtended {

    TextureType canpipe_getType();
    void canpipe_setMipmapMode(FilterMode filterMode);
    void canpipe_setAddressModeR(AddressMode addressMode);
    void canpipe_setCompareOp(TextureCompareOp compareOp);

}
