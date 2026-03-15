package fewizz.canpipe.b3d;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;

public interface GpuSamplerExtended {

    AddressMode canpipe_getAddressModeW();
    @Nullable CompareOp canpipe_getCompareOp();

}
