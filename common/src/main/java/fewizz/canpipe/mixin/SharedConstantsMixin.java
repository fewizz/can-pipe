package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.SharedConstants;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {

    // Used by GlPipelineRecompiler.SHADER_DEBUG_MODE
    /*@Shadow public static boolean IS_RENDERDOC_ATTACHED = false;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void onClassInit(CallbackInfo ci) {
        IS_RENDERDOC_ATTACHED = true;
    }*/

}
