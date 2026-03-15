package fewizz.canpipe.compat.cinnabar.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuSampler;

import fewizz.canpipe.b3d.GpuSamplerExtended;
import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.api.hg.enums.HgCompareOp;
import graphics.cinnabar.core.hg3d.Hg3DConst;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuSampler;

@Mixin(Hg3DGpuSampler.class)
public abstract class Hg3DGpuSamplerMixin extends GpuSampler implements GpuSamplerExtended {

    @Unique protected AddressMode canpipe_addressModeW;
    @Unique @Nullable protected CompareOp canpipe_compareOp = null;
    @Unique protected boolean canpipe_linearMipmap;

    @Override
    public AddressMode canpipe_getAddressModeW() {
        return this.canpipe_addressModeW;
    }

    @Override
    public @Nullable CompareOp canpipe_getCompareOp() {
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
    HgSampler.CreateInfo beforeMercurySamplerCreation(HgSampler.CreateInfo createInfo, @Local Hg3DGpuDevice device) {
        var deviceAccessor = (Hg3DGpuDeviceAccessor) device;

        this.canpipe_addressModeW = deviceAccessor.get_canpipe_addressModeW();
        this.canpipe_compareOp = deviceAccessor.get_canpipe_compareOp();

        HgCompareOp hgCompareOp = this.canpipe_compareOp == null ? createInfo.compareOp() : switch (this.canpipe_compareOp) {
            case CompareOp.ALWAYS_PASS -> HgCompareOp.ALWAYS;  // If specified, VkSamplerCreateInfo.compareEnable will be false, which is... fine?
            case CompareOp.NEVER_PASS -> HgCompareOp.NEVER;
            case CompareOp.EQUAL -> HgCompareOp.EQUAL;
            case CompareOp.NOT_EQUAL -> HgCompareOp.NOT_EQUAL;
            case CompareOp.LESS_THAN -> HgCompareOp.LESS;
            case CompareOp.LESS_THAN_OR_EQUAL -> HgCompareOp.LESS_OR_EQUAL;
            case CompareOp.GREATER_THAN -> HgCompareOp.GREATER;
            case CompareOp.GREATER_THAN_OR_EQUAL -> HgCompareOp.GREATER_OR_EQUAL;
        };

        int addressW = this.canpipe_addressModeW == null ? createInfo.addressW() : Hg3DConst.addressMode(canpipe_addressModeW);
        // boolean mip = deviceAccessor.get_canpipe_linearMipmap() == null ? createInfo.mip() : deviceAccessor.get_canpipe_linearMipmap();

        return new HgSampler.CreateInfo(
            createInfo.minLinear(),
            createInfo.magLinear(),
            createInfo.addressU(),
            createInfo.addressV(),
            addressW,
            createInfo.mip(),
            hgCompareOp,
            createInfo.maxAnisotropy()
        );
    }

}
