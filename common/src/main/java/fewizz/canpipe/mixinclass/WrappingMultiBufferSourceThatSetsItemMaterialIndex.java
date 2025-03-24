package fewizz.canpipe.mixinclass;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.CanPipe;
import fewizz.canpipe.material.MaterialMap;
import fewizz.canpipe.material.MaterialMaps;
import fewizz.canpipe.mixininterface.VertexConsumerExtended;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

/** Used only by {@link fewizz.canpipe.mixin.ItemStackLayerRenderStateMixin ItemStackLayerRenderStateMixin} */
public class WrappingMultiBufferSourceThatSetsItemMaterialIndex implements MultiBufferSource {

    MultiBufferSource source;
    Item item;

    public WrappingMultiBufferSourceThatSetsItemMaterialIndex(MultiBufferSource source, Item item) {
        this.source = source;
        this.item = item;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        var vertexConsumer = this.source.getBuffer(renderType);
        if (
            vertexConsumer instanceof BufferBuilder bb &&
            bb.format.contains(CanPipe.VertexFormatElements.MATERIAL_INDEX)
        ) {
            MaterialMap materialMap = null;
            if (this.item instanceof BlockItem bi) {
                materialMap = MaterialMaps.INSTANCE.getForBlock(bi.getBlock());
            }
            ((VertexConsumerExtended) bb).canpipe_setSharedMaterialMap(materialMap);
        }
        return vertexConsumer;
    }

}
