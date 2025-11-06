package fewizz.canpipe.compat.cinnabar.mixin;

import org.lwjgl.util.spvc.Spvc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import graphics.cinnabar.core.mercury.MercuryShaderSet;

@Mixin(MercuryShaderSet.class)
public class MercuryShaderSetMixin {

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/spvc/Spvc;spvc_resources_get_resource_list_for_type(JILorg/lwjgl/PointerBuffer;Lorg/lwjgl/PointerBuffer;)I",
            ordinal = 1  // first is for VS
        ),
        index = 1
    )
    int fixSPVCConstForFragShaderOutput(int original) {
        assert original == Spvc.SPVC_BUILTIN_RESOURCE_TYPE_STAGE_OUTPUT;
        return Spvc.SPVC_RESOURCE_TYPE_STAGE_OUTPUT;
    }

}
