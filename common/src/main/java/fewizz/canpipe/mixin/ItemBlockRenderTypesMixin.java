package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    @ModifyExpressionValue(
        method = "getRenderType(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/renderer/RenderType;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;translucentItemSheet()Lnet/minecraft/client/renderer/RenderType;"
        )
    )
    private static RenderType replaceItemStackTranslucentItemSheetWithSolidOne(RenderType renderType) {
        if (Pipelines.getCurrent() != null) {
            renderType = Sheets.cutoutBlockSheet();
        }
        return renderType;
    }

}
