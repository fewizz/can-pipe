package fewizz.canpipe.compat.cinnabar.mixin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.b3d.GpuTextureExtended;
import graphics.cinnabar.api.hg.HgSampler;
import graphics.cinnabar.core.hg3d.Hg3DConst;
import graphics.cinnabar.core.hg3d.Hg3DGpuDevice;
import graphics.cinnabar.core.hg3d.Hg3DGpuTexture;

@Mixin(Hg3DGpuTexture.class)
public abstract class Hg3DGpuTextureMixin extends GpuTexture implements GpuTextureExtended {

    public Hg3DGpuTextureMixin() { super(-1, null, null, -1, -1, -1, -1); }

    @Shadow @Final private Hg3DGpuDevice device;

    @Unique @NotNull protected AddressMode canpipe_addressModeW = AddressMode.REPEAT;
    @Unique @Nullable protected DepthTestFunction canpipe_compareOp = null;

    @Override
    public void canpipe_setAddressModeW(@NotNull AddressMode addressMode) {
        this.canpipe_addressModeW = addressMode;
    }

    @Override
    public void canpipe_setCompareOp(@Nullable DepthTestFunction compareOp) {
        this.canpipe_compareOp = compareOp;
    }

    @ModifyArg(
        method = "sampler",
        at = @At(
            value = "INVOKE",
            target = "Lgraphics/cinnabar/core/hg3d/Hg3DGpuDevice;getSampler(ZZIIIZ)Lgraphics/cinnabar/api/hg/HgSampler;"
        ),
        index = 4
    )
    int setAddressModeW(int addressModeW) {
        assert addressModeW == 0;
        return Hg3DConst.addressMode(this.canpipe_addressModeW != null ? this.canpipe_addressModeW : AddressMode.REPEAT);
    }

    @Inject(method = "sampler", at = @At("HEAD"), cancellable = true)
    void getSamplerExteneded(CallbackInfoReturnable<HgSampler> cir) {
        if (this.canpipe_compareOp == null) {
            return;  // Original path, using precreated samplers
        }

        /*cir.setReturnValue(((Hg3DGpuDeviceAccessor) this.device).canpipe_getSampler(
            this.minFilter == FilterMode.LINEAR,
            this.magFilter == FilterMode.LINEAR,
            Hg3DConst.addressMode(this.addressModeU),
            Hg3DConst.addressMode(this.addressModeV),
            Hg3DConst.addressMode(this.canpipe_addressModeW != null ? this.canpipe_addressModeW : AddressMode.REPEAT),
            this.useMipmaps,
            this.canpipe_compareOp   // added
        ));*/
    }

}
