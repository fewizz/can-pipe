package fewizz.canpipe;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class VertexFormatElements {
    public static final VertexFormatElement AO;
    public static final VertexFormatElement MATERIAL;
    public static final VertexFormatElement TANGENT;

    static {
        AO = VertexFormatElement.register(6, 0, com.mojang.blaze3d.vertex.VertexFormatElement.Type.FLOAT, com.mojang.blaze3d.vertex.VertexFormatElement.Usage.GENERIC, 1);
        MATERIAL = VertexFormatElement.register(7, 0, com.mojang.blaze3d.vertex.VertexFormatElement.Type.INT, com.mojang.blaze3d.vertex.VertexFormatElement.Usage.GENERIC, 1);
        TANGENT = VertexFormatElement.register(8, 0, com.mojang.blaze3d.vertex.VertexFormatElement.Type.BYTE, com.mojang.blaze3d.vertex.VertexFormatElement.Usage.NORMAL, 3);
    }
}
