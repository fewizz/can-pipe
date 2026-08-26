package fewizz.canpipe.renderpearl;

import org.jspecify.annotations.Nullable;

import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.textures.AddressMode;

public interface GpuSamplerExtended {

    AddressMode canpipe_getAddressModeW();
    @Nullable CompareOp canpipe_getCompareOp();

}
