package fewizz.canpipe.b3d;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;

public interface GpuTextureExtended {

    void canpipe_setAddressModeW(@NotNull AddressMode addressMode);
    void canpipe_setCompareOp(@Nullable DepthTestFunction compareOp);

}
