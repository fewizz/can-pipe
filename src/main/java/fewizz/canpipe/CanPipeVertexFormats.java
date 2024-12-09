package fewizz.canpipe;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CanPipeVertexFormats {

    public static final VertexFormat BLOCK = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)  // 0
        .add("Color", VertexFormatElement.COLOR)  // 12 + 4 = 16
        .add("UV0", VertexFormatElement.UV0)  // 16
        .add("UV2", VertexFormatElement.UV2)  // 24
        .add("Normal", VertexFormatElement.NORMAL) // 28 + 4 = 32
        .padding(1)
        .add("AO", CanPipeVertexFormatElements.AO)  // 32
        .add("SpriteIndex", CanPipeVertexFormatElements.SPRITE_INDEX)  // 36
        .add("MaterialIndex", CanPipeVertexFormatElements.MATERIAL_INDEX)  // 40
        .add("Tangent", CanPipeVertexFormatElements.TANGENT)  // 44
        .build();

    public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV1", VertexFormatElement.UV1)
        .add("UV2", VertexFormatElement.UV2)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .add("SpriteIndex", CanPipeVertexFormatElements.SPRITE_INDEX)
        .add("MaterialIndex", CanPipeVertexFormatElements.MATERIAL_INDEX)
        .add("Tangent", CanPipeVertexFormatElements.TANGENT)
        .build();

    public static final VertexFormat PARTICLE = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV2", VertexFormatElement.UV2)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(1)
        .add("SpriteIndex", CanPipeVertexFormatElements.SPRITE_INDEX)
        .add("MaterialIndex", CanPipeVertexFormatElements.MATERIAL_INDEX)
        .add("Tangent", CanPipeVertexFormatElements.TANGENT)
        .build();

}
