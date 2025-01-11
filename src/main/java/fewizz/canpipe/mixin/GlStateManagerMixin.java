package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.mojang.blaze3d.platform.GlStateManager;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 12))
    private static int increaseTextureCount(int original) {
        return 16;
    }

}
