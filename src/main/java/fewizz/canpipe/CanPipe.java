package fewizz.canpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import blue.endless.jankson.Jankson;

public class CanPipe {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Jankson JANKSON = Jankson.builder().build();

    public static class VertexFormatElements {

        public static final VertexFormatElement AO =
            VertexFormatElement.register(
                6, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 1
            );

        public static final VertexFormatElement SPRITE_INDEX =
            VertexFormatElement.register(
                7, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            );  // UV, because it uses vertexAttrib *I* Pointer in this case

        public static final VertexFormatElement MATERIAL_INDEX =
            VertexFormatElement.register(
                8, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            );

        public static final VertexFormatElement TANGENT =
            VertexFormatElement.register(
                9, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 4
            );

    }

    public class VertexFormats {

        public static final VertexFormat BLOCK = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)  // 0
            .add("Color", VertexFormatElement.COLOR)  // 12 + 4 = 16
            .add("UV0", VertexFormatElement.UV0)  // 16
            .add("UV2", VertexFormatElement.UV2)  // 24
            .add("Normal", VertexFormatElement.NORMAL) // 28 + 4 = 32
            .padding(1)
            .add("AO", CanPipe.VertexFormatElements.AO)  // 32
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)  // 36
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)  // 40
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)  // 44
            .build();

        public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

    }

}