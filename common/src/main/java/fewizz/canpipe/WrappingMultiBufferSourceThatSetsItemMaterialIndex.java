package fewizz.canpipe;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class WrappingMultiBufferSourceThatSetsItemMaterialIndex implements MultiBufferSource {

    MultiBufferSource source;
    Item item;

    public WrappingMultiBufferSourceThatSetsItemMaterialIndex(MultiBufferSource source, Item item) {
        this.source = source;
        this.item = item;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        var vc = source.getBuffer(renderType);
        if (vc instanceof BufferBuilder bb && bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)) {
            MaterialMap materialMap = null;
            if (item instanceof BlockItem bi) {
                ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                materialMap = MaterialMaps.BLOCKS.get(rl);
            }
            ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(materialMap);
        }
        return vc;
    }

}
