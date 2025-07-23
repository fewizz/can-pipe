package fewizz.canpipe.neoforge.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.b3d.GpuTextureExtended;
import net.neoforged.neoforge.client.blaze3d.validation.GpuDeviceUsageValidator;
import net.neoforged.neoforge.client.blaze3d.validation.ValidationGpuTexture;

@Mixin(ValidationGpuTexture.class)
public abstract class ValidationGpuTextureMixin implements GpuTextureExtended {
    @Shadow @Final private GpuTexture realTexture;
    @Shadow @Final private GpuDeviceUsageValidator validator;

    @Override
    public void canpipe_setMipmapMode(FilterMode filterMode) {
        ((GpuTextureExtended) realTexture).canpipe_setMipmapMode(filterMode);
    }

    @Override
    public void canpipe_setAddressModeW(AddressMode addressMode) {
        ((GpuTextureExtended) realTexture).canpipe_setAddressModeW(addressMode);
    }

    @Override
    public void canpipe_setCompareOp(DepthTestFunction compareOp) {
        ((GpuTextureExtended) realTexture).canpipe_setCompareOp(compareOp);
    }

}
