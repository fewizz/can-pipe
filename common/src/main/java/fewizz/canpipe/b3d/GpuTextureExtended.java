package fewizz.canpipe.b3d;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;

public interface GpuTextureExtended {

    void canpipe_setMipmapMode(@Nullable FilterMode filterMode);
    void canpipe_setAddressModeW(@Nullable AddressMode addressMode);
    void canpipe_setCompareOp(@Nullable DepthTestFunction compareOp);

}
