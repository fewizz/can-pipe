package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import fewizz.canpipe.CanPipeScheets;
import fewizz.canpipe.Pipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

@Mixin(Sheets.class)
public class SheetsMixin {

    @ModifyReturnValue(method = "cutoutBlockSheet", at = @At("RETURN"))
    private static RenderType replaceCutoutBlockSheet(RenderType original) {
        return Pipelines.getCurrent() != null ? CanPipeScheets.CUTOUT_BLOCK_SHEET : original;
    }

}
