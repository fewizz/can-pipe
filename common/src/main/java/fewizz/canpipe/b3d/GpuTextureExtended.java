package fewizz.canpipe.b3d;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

public interface GpuTextureExtended {

    void canpipe_setMipmapMode(FilterMode filterMode);
    void canpipe_setAddressModeW(AddressMode addressMode);
    void canpipe_setCompareOp(DepthTestFunction compareOp);

}
