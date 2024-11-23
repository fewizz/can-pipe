package fewizz.canpipe;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CanPipeVertexFormatElements {
    public static final VertexFormatElement AO;
    public static final VertexFormatElement MATERIAL;
    public static final VertexFormatElement TANGENT;

    static {
        AO = VertexFormatElement.register(6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);
        MATERIAL = VertexFormatElement.register(7, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.GENERIC, 1);
        TANGENT = VertexFormatElement.register(8, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);
    }
}
