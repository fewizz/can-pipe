package fewizz.canpipe.compat.cinnabar.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuSampler;

import fewizz.canpipe.b3d.GpuSamplerExteneded;
import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.api.hg.enums.HgCompareOp;
import graphics.cinnabar.core.hg3d.Hg3DConst;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuSampler;

@Mixin(Hg3DGpuSampler.class)
public abstract class Hg3DGpuSamplerMixin extends GpuSampler implements GpuSamplerExteneded {

    @Unique protected AddressMode canpipe_addressModeW;
    @Unique @Nullable protected DepthTestFunction canpipe_compareOp = null;
    @Unique protected boolean canpipe_linearMipmap;

    @Override
    public AddressMode canpipe_getAddressModeW() {
        return this.canpipe_addressModeW;
    }

    @Override
    public @Nullable DepthTestFunction canpipe_getCompareOp() {
        return this.canpipe_compareOp;
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/api/hg/HgDevice;createSampler("+
                "Lgraphics/cinnabar/api/hg/HgSampler$CreateInfo;"+
            ")Lgraphics/cinnabar/api/hg/HgSampler;"
        ),
        index = 0
    )
    HgSampler.CreateInfo beforeMecurySamplerCreation(HgSampler.CreateInfo createInfo, @Local Hg3DGpuDevice device) {
        var deviceAccessor = (Hg3DGpuDeviceAccessor) device;

        this.canpipe_addressModeW = deviceAccessor.get_canpipe_addressModeW();
        this.canpipe_compareOp = deviceAccessor.get_canpipe_compareOp();

        HgCompareOp hgCompareOp = this.canpipe_compareOp == null ? createInfo.compareOp() : switch (this.canpipe_compareOp) {
            case DepthTestFunction.NO_DEPTH_TEST -> HgCompareOp.ALWAYS;  // If specified, VkSamplerCreateInfo.compareEnable will be false, which is... fine?
            case DepthTestFunction.EQUAL_DEPTH_TEST -> HgCompareOp.EQUAL;
            case DepthTestFunction.LEQUAL_DEPTH_TEST -> HgCompareOp.LESS_OR_EQUAL;
            case DepthTestFunction.LESS_DEPTH_TEST -> HgCompareOp.LESS;
            case DepthTestFunction.GREATER_DEPTH_TEST -> HgCompareOp.GREATER;
        };

        int addressW = this.canpipe_addressModeW == null ? createInfo.addressW() : Hg3DConst.addressMode(canpipe_addressModeW);
        boolean mip = deviceAccessor.get_canpipe_linearMipmap() == null ? createInfo.mip() : deviceAccessor.get_canpipe_linearMipmap();

        return new HgSampler.CreateInfo(
            createInfo.minLinear(),
            createInfo.magLinear(),
            createInfo.addressU(),
            createInfo.addressV(),
            addressW,
            mip,
            hgCompareOp,
            createInfo.maxAnisotropy()
        );
    }

}
