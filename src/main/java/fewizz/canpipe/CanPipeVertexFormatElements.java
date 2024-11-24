package fewizz.canpipe;

import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CanPipeVertexFormatElements {
    public static final VertexFormatElement AO;
    public static final VertexFormatElement SPRITE_INDEX;
    public static final VertexFormatElement TANGENT;

    static {
        AO = VertexFormatElement.register(6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1);
        SPRITE_INDEX = VertexFormatElement.register(7, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1);  // UV, because it uses vertexAttrib *I* Pointer in this case
        TANGENT = VertexFormatElement.register(8, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 4);
    }
}
