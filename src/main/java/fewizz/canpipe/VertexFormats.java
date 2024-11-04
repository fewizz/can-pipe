package fewizz.canpipe;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class VertexFormats {

    public static final VertexFormat BLOCK = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("AO", VertexFormatElements.AO)
            .add("Material", VertexFormatElements.MATERIAL)
            .add("Tangent", VertexFormatElements.TANGENT)
            .padding(1)
            .build();

}
