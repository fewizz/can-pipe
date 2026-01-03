package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.ItemBlockRenderTypes;

@Mixin(ItemBlockRenderTypes.class)
public class ItemBlockRenderTypesMixin {

    /*@ModifyExpressionValue(
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
    }*/ // TODO

}
