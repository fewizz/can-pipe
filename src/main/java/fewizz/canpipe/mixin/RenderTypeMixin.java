package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.CanPipeRenderTypes;
import fewizz.canpipe.Mod;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Mixin(RenderType.class)
public class RenderTypeMixin {

    // For now just replacing, but i think it would be better to replace them locally, TODO?

    @ModifyReturnValue(method = "solid", at = @At("RETURN"))
    private static RenderType replaceSolid(RenderType original) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.SOLID : original;
    }

    @ModifyReturnValue(method = "cutoutMipped", at = @At("RETURN"))
    private static RenderType replaceCutoutMipped(RenderType original) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.CUTOUT_MIPPED : original;
    }

    @ModifyReturnValue(method = "cutout", at = @At("RETURN"))
    private static RenderType replaceCutout(RenderType original) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.CUTOUT : original;
    }

    @ModifyReturnValue(method = "translucent", at = @At("RETURN"))
    private static RenderType replaceTranslucent(RenderType original) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.TRANSLUCENT : original;
    }

    @ModifyReturnValue(method = "entitySolid", at = @At("RETURN"))
    private static RenderType replaceEntitySolid(RenderType original, ResourceLocation loc) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.ENTITY_SOLID.apply(loc) : original;
    }

    @ModifyReturnValue(method = "entityCutout", at = @At("RETURN"))
    private static RenderType replaceEntityCutout(RenderType original, ResourceLocation loc) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.ENTITY_CUTOUT.apply(loc) : original;
    }

    @ModifyReturnValue(method = "entityCutoutNoCull", at = @At("RETURN"))
    private static RenderType replaceEntityCutoutNoCull(RenderType original, ResourceLocation loc, boolean b) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.ENTITY_CUTOUT_NO_CULL.apply(loc, b) : original;
    }

}
