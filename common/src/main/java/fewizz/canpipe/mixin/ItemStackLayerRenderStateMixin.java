package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import fewizz.canpipe.WrappingMultiBufferSourceThatSetsItemMaterialIndex;
import fewizz.canpipe.mixininterface.ItemStackRenderStateExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemStackLayerRenderStateMixin {

    @Shadow(aliases = {"this$0"})
    ItemStackRenderState field_55345;

    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem("+
                "Lnet/minecraft/world/item/ItemDisplayContext;"+  // 0
                "Lcom/mojang/blaze3d/vertex/PoseStack;"+  // 1
                "Lnet/minecraft/client/renderer/MultiBufferSource;"+  // 2
                "II[I"+
                "Lnet/minecraft/client/resources/model/BakedModel;"+
                "Lnet/minecraft/client/renderer/RenderType;"+
                "Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;"+
            ")V"
        ),
        index = 2
    )
    MultiBufferSource replaceBufferSourceOnRender(
        MultiBufferSource source
    ) {
        return new WrappingMultiBufferSourceThatSetsItemMaterialIndex(
            source,
            ((ItemStackRenderStateExtended) this.field_55345).getItem()
        );
    }

}
