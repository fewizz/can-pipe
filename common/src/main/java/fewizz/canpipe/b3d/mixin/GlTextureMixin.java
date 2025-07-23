package fewizz.canpipe.b3d.mixin;

import org.lwjgl.opengl.GL33C;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.b3d.GpuTextureExtended;

@Mixin(GlTexture.class)
public abstract class GlTextureMixin extends GpuTexture implements GpuTextureExtended {

    public GlTextureMixin() {
        super(0, null, null, 0, 0, 0, 0);
    }

    @Shadow protected boolean modesDirty;

    @Unique protected FilterMode canpipe_mipFilter = null;
    @Unique protected AddressMode canpipe_addressModeW = null;
    @Unique protected DepthTestFunction canpipe_compareOp = null;

    @Override
    public void canpipe_setMipmapMode(FilterMode filterMode) {
        this.canpipe_mipFilter = filterMode;
        this.useMipmaps = filterMode != null;
        this.modesDirty = true;
    }

    @Override
    public void canpipe_setAddressModeW(AddressMode addressMode) {
        this.canpipe_addressModeW = addressMode;
        this.modesDirty = true;
    }

    @Override
    public void canpipe_setCompareOp(DepthTestFunction compareOp) {
        this.canpipe_compareOp = compareOp;
        this.modesDirty = true;
    }

    @ModifyExpressionValue(
        method = "flushModeChanges",
        at = @At(value = "CONSTANT", args = "intValue=9986")  // GL_NEAREST_MIPMAP_LINEAR
    )
    private int onMipmapMinNearestFilter(int value) {
        if (this.canpipe_mipFilter == FilterMode.NEAREST) {
            value = GL33C.GL_NEAREST_MIPMAP_NEAREST;
        }
        return value;
    }

    @ModifyExpressionValue(
        method = "flushModeChanges",
        at = @At(value = "CONSTANT", args = "intValue=9987")  // GL_LINEAR_MIPMAP_LINEAR
    )
    private int onMipmapMinLinearFilter(int value) {
        if (this.canpipe_mipFilter == FilterMode.NEAREST) {
            value = GL33C.GL_LINEAR_MIPMAP_NEAREST;
        }
        return value;
    }

    @Inject(
        method = "flushModeChanges",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texParameter(III)V",
            ordinal = 1
        )
    )
    private void afterSettingVAddressingMode(int target, CallbackInfo ci) {
        if (this.canpipe_addressModeW != null) {
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_WRAP_R, GlConst.toGl(this.canpipe_addressModeW));
        }
    }

    @Inject(
        method = "flushModeChanges",
        at = @At(
            value = "FIELD",
            opcode = Opcodes.PUTFIELD,
            target = "Lcom/mojang/blaze3d/opengl/GlTexture;modesDirty:Z",
            shift = Shift.BEFORE
        )
    )
    private void beforeModesDirtyFalse(int target, CallbackInfo ci) {
        if (this.canpipe_compareOp != null) {
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_COMPARE_MODE, GL33C.GL_COMPARE_REF_TO_TEXTURE);
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_COMPARE_FUNC, GlConst.toGl(this.canpipe_compareOp));
        }
        else {
            GlStateManager._texParameter(target, GL33C.GL_TEXTURE_COMPARE_MODE, GL33C.GL_NONE);
        }
    }

}
