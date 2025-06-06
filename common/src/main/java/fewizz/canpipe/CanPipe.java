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
    public static final KeyMapping PIPELINE_IO_DEBUG = new KeyMapping(
        "can-pipe.key.pipelineIODebug", GLFW.GLFW_KEY_UNKNOWN, "can-pipe.key.categories.can-pipe"
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

        public static final VertexFormat BLOCK = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .add("AO", CanPipe.VertexFormatElements.AO)
            .build();

        public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .add("MaterialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .add("SpriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            .add("Tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

    }

}