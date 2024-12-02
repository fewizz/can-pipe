package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.CanPipeRenderTypes;
import fewizz.canpipe.Mod;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.renderer.RenderType;

@Mixin(value = BlockRenderInfo.class, remap = false)
public class BlockRenderInfoMixin {

    @ModifyReturnValue(method = "effectiveRenderLayer", at = @At("RETURN"))
    private RenderType replaceRenderType(RenderType original) {
        return Mod.getCurrentPipeline() != null ? CanPipeRenderTypes.replaced(original) : original;
    }

}
