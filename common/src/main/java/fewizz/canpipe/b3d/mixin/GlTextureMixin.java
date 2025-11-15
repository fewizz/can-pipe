package fewizz.canpipe.b3d.mixin;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL33C;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.GpuTexture;

import blue.endless.jankson.annotation.Nullable;
import fewizz.canpipe.b3d.GpuTextureExtended;

@Mixin(GlTexture.class)
public abstract class GlTextureMixin extends GpuTexture implements GpuTextureExtended {

    public GlTextureMixin() { super(0, null, null, 0, 0, 0, 0); }

    @Shadow protected boolean modesDirty;

    @Unique @NotNull protected AddressMode canpipe_addressModeW = AddressMode.REPEAT;
    @Unique @Nullable protected DepthTestFunction canpipe_compareOp = null;

    @Override
    public void canpipe_setAddressModeW(@NotNull AddressMode addressMode) {
        this.canpipe_addressModeW = addressMode;
        this.modesDirty = true;
    }

    @Override
    public void canpipe_setCompareOp(DepthTestFunction compareOp) {
        this.canpipe_compareOp = compareOp;
        this.modesDirty = true;
    }

    @ModifyConstant(
        method = "flushModeChanges",
        constant = @Constant(
            intValue = GL11C.GL_NEAREST,
            ordinal = 0  // min filter
        )
    )
    private int onMinNearestFilter(int value) {
        if (this.getMipLevels() > 1) {
            value = GL33C.GL_NEAREST_MIPMAP_NEAREST;
        }
        return value;
    }

    @ModifyConstant(
        method = "flushModeChanges",
        constant = @Constant(
            intValue = GL11C.GL_LINEAR,
            ordinal = 0  // min filter
        )
    )
    private int onMinLinearFilter(int value) {
        if (this.getMipLevels() > 1) {
            value = GL33C.GL_LINEAR_MIPMAP_NEAREST;
        }
        return value;
    }

    @Inject(
        method = "flushModeChanges",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texParameter(III)V",
            ordinal = 1,  // right after setting GL_TEXTURE_WRAP_T
            shift = Shift.AFTER
        )
    )
    private void afterSettingTextureWrapT(int target, CallbackInfo ci) {
        GlStateManager._texParameter(target, GL33C.GL_TEXTURE_WRAP_R, GlConst.toGl(this.canpipe_addressModeW));
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
