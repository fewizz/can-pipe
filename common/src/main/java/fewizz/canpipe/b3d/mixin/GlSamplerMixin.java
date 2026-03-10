package fewizz.canpipe.b3d.mixin;

import java.util.OptionalDouble;

import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.textures.AddressMode;

import fewizz.canpipe.b3d.GpuSamplerExteneded;
import fewizz.canpipe.b3d.RealGpuDeviceProviderService;

@Mixin(GlSampler.class)
public abstract class GlSamplerMixin implements GpuSamplerExteneded {

    @Shadow @Final private int id;
    @Shadow @Final private OptionalDouble maxLod;

    @Unique protected AddressMode canpipe_addressModeW;
    @Unique @Nullable protected CompareOp canpipe_compareOp = null;
    @Unique protected boolean canpipe_linearMipmap;

    @Inject(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGenSamplers()I")
    )
    void onInitBegin(CallbackInfo ci) {
        GpuDeviceBackend device = RealGpuDeviceProviderService.getRealGpuDeviceBackend();
        this.canpipe_addressModeW = ((GlDeviceAccessor) device).get_canpipe_addressModeW();
        this.canpipe_linearMipmap = ((GlDeviceAccessor) device).get_canpipe_linearMipmap() != null ? ((GlDeviceAccessor) device).get_canpipe_linearMipmap() : true;
        if (this.canpipe_addressModeW == null) {
            this.canpipe_addressModeW = AddressMode.REPEAT;
        }
        this.canpipe_compareOp = ((GlDeviceAccessor) device).get_canpipe_compareOp();
    }

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    void onInitEnd(CallbackInfo ci) {
        if (this.canpipe_compareOp != null) {
            GL33C.glSamplerParameteri(this.id, GL33C.GL_TEXTURE_COMPARE_MODE, GL33C.GL_COMPARE_REF_TO_TEXTURE);
            GL33C.glSamplerParameteri(this.id, GL33C.GL_TEXTURE_COMPARE_FUNC, GlConst.toGl(this.canpipe_compareOp));
        }
        else {
            GL33C.glSamplerParameteri(this.id, GL33C.GL_TEXTURE_COMPARE_MODE, GL33C.GL_NONE);
        }

        GL33C.glSamplerParameteri(this.id, GL33C.GL_TEXTURE_WRAP_R, GlConst.toGl(this.canpipe_addressModeW));
    }

    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = GL11C.GL_NEAREST_MIPMAP_LINEAR)
    )
    private int onMinNearestFilter(int value) {
        if (!this.canpipe_linearMipmap) {
            value = GL33C.GL_NEAREST_MIPMAP_NEAREST;
        }
        return value;
    }

    @ModifyConstant(
        method = "<init>",
        constant = @Constant(intValue = GL11C.GL_LINEAR_MIPMAP_LINEAR)
    )
    private int onMinLinearFilter(int value) {
        if (!this.canpipe_linearMipmap) {
            value = GL33C.GL_LINEAR_MIPMAP_NEAREST;
        }
        return value;
    }

    @Override
    public AddressMode canpipe_getAddressModeW() {
        return this.canpipe_addressModeW;
    }

    @Override
    public @Nullable CompareOp canpipe_getCompareOp() {
        return this.canpipe_compareOp;
    }

}
