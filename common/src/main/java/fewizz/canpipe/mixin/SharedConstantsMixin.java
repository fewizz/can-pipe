package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.SharedConstants;

// TODO remove
@Mixin(SharedConstants.class)
public class SharedConstantsMixin {

    @Shadow public static boolean IS_RENDERDOC_ATTACHED = false;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onClassInit(CallbackInfo ci) {
        IS_RENDERDOC_ATTACHED = true;
    }

}
