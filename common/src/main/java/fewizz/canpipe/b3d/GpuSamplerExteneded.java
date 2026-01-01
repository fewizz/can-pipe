package fewizz.canpipe.b3d;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;

public interface GpuSamplerExteneded {

    abstract AddressMode canpipe_getAddressModeW();
    @Nullable abstract DepthTestFunction canpipe_getCompareOp();

}
