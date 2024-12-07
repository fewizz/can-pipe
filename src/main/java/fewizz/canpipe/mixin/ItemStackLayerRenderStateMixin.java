package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipeVertexFormatElements;
import fewizz.canpipe.Material;
import fewizz.canpipe.MaterialMap;
import fewizz.canpipe.MaterialMaps;
import fewizz.canpipe.Materials;
import fewizz.canpipe.mixininterface.ItemStackRenderStateAccessor;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class ItemStackLayerRenderStateMixin {

    @Shadow
    ItemStackRenderState this$0;

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
        return new MultiBufferSource() {

            @Override
            public VertexConsumer getBuffer(RenderType renderType) {
                var vc = source.getBuffer(renderType);
                Item item = ((ItemStackRenderStateAccessor) this$0).getItem();
                if (vc instanceof BufferBuilder bb && bb.format.contains(CanPipeVertexFormatElements.MATERIAL_INDEX)) {
                    int index = -1;
                    if (item instanceof BlockItem bi) {
                        ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                        MaterialMap materialMap = MaterialMaps.BLOCKS.get(rl);
                        Material material = materialMap != null ? materialMap.defaultMaterial : null;
                        if (material != null) {
                            index = Materials.id(material);
                        }
                    }
                    ((VertexConsumerExtended) bb).setSharedMaterialIndex(index);
                }
                return vc;
            }
            
        };
    }

}
