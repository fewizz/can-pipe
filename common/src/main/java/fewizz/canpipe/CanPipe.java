package fewizz.canpipe;

import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import blue.endless.jankson.Jankson;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class CanPipe {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger("can-pipe");
    public static final Jankson JANKSON = Jankson.builder().build();
    public static final KeyMapping PIPELINES_RELOAD_KEY = new KeyMapping(
        "can-pipe.key.reloadPipelines", GLFW.GLFW_KEY_UNKNOWN, "can-pipe.key.categories.can-pipe"
    );

    public static Path getCompilationErrorsDirPath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("can-pipe-compilation-errors");
    }

    public static Path getConfigurationFilePath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("config/can-pipe.json");
    }

    public static class VertexFormatElements {

        public static final VertexFormatElement
            MATERIAL_FLAGS = VertexFormatElement.register(
                6, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.UV, 1
            ),  // UV, because it uses vertexAttrib *I* Pointer in this case
            SPRITE_INDEX = VertexFormatElement.register(
                8, 0, VertexFormatElement.Type.INT, VertexFormatElement.Usage.UV, 1
            ),  // UV, because it uses vertexAttrib *I* Pointer in this case
            MATERIAL_INDEX = VertexFormatElement.register(
                9, 0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 1
            ),
            TANGENT = VertexFormatElement.register(
                10, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 4
            ),
            AO = VertexFormatElement.register(
                7, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 1
            );

    }

    public class VertexFormats {

        /* size % 4 should be == 0, for quads sorting */
        public static final VertexFormat BLOCK = VertexFormat.builder()
            /* 0  + 3*4 */.add("Position", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("Color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("UV0", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("UV2", VertexFormatElement.UV2)
            /* 28 + 3*1 */.add("Normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.padding(1)

            /* 32 + 1*4 */.add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 36 + 4*1 */.add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 40 + 1*2 */.add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 42 + 1*1 */.add("AO", CanPipe.VertexFormatElements.AO)
            /* 43 + 1*1 */.add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .build();

        public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
            /* 0  + 3*4 */.add("Position", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("Color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("UV0", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("UV1", VertexFormatElement.UV1)
            /* 28 + 2*2 */.add("UV2", VertexFormatElement.UV2)
            /* 32 + 3*1 */.add("Normal", VertexFormatElement.NORMAL)

            /* 35 + 1*1 */.add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 36 + 1*4 */.add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            // .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 40 + 1*4 */.add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            /* 0  + 3*4 */.add("Position", VertexFormatElement.POSITION)
            /* 12 + 2*4 */.add("UV0", VertexFormatElement.UV0)
            /* 20 + 4*1 */.add("Color", VertexFormatElement.COLOR)
            /* 24 + 2*2 */.add("UV2", VertexFormatElement.UV2)

            /* 28 + 3*1 */.add("Normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 32 + 1*4 */.add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            // .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 36 + 4*1 */.add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

    }

}