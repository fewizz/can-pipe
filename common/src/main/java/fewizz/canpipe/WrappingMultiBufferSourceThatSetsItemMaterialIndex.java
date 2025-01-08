package fewizz.canpipe;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.material.Materials;
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
            int index = -1;
            if (item instanceof BlockItem bi) {
                ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                MaterialMap materialMap = MaterialMaps.BLOCKS.get(rl);
                Material material = materialMap != null ? materialMap.defaultMaterial : null;
                if (material != null) {
                    index = Materials.id(material);
                }
            }
            ((VertexConsumerExtended) bb).canpipe_setSharedMaterialIndex(index);
        }
        return vc;
    }

}
