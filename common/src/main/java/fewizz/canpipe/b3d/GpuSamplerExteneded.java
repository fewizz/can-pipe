package fewizz.canpipe.b3d;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuSampler;

public abstract class GpuSamplerExteneded extends GpuSampler {

    abstract void canpipe_setAddressModeW(@NotNull AddressMode addressMode);
    abstract void canpipe_setCompareOp(@Nullable DepthTestFunction compareOp);

}
