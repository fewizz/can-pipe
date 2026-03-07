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
import net.minecraft.resources.Identifier;

public class CanPipe {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger("can-pipe");
    public static final Jankson JANKSON = Jankson.builder().build();
    public static final KeyMapping PIPELINES_RELOAD_KEY = new KeyMapping(
        "can-pipe.key.reloadPipelines",
        GLFW.GLFW_KEY_UNKNOWN,
        new KeyMapping.Category(Identifier.parse("canpipe:key.categories.can-pipe"))
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
            MATERIAL_FLAGS = VertexFormatElement.register(7, 0, VertexFormatElement.Type.BYTE, false, 1),  // UV, because it uses vertexAttrib *I* Pointer in this case
            SPRITE_INDEX = VertexFormatElement.register(8, 0, VertexFormatElement.Type.INT, false, 1),  // UV, because it uses vertexAttrib *I* Pointer in this case
            MATERIAL_INDEX = VertexFormatElement.register(9, 0, VertexFormatElement.Type.SHORT, false, 1),
            TANGENT = VertexFormatElement.register(10, 0, VertexFormatElement.Type.BYTE, true, 4),
            AO = VertexFormatElement.register(11, 0, VertexFormatElement.Type.UBYTE, true, 1);

    }

    public class VertexFormats {

        /* size % 4 should be == 0, for quads sorting */
        public static final VertexFormat BLOCK = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_lightmap", VertexFormatElement.UV2)
            /* 28 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.padding(1)

            /* 32 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 36 + 4*1 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 40 + 1*2 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 42 + 1*1 */.add("in_ao", CanPipe.VertexFormatElements.AO)
            /* 43 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 44 + 4*1 */.padding(4)
            .build();

        public static final VertexFormat ENTITY = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_overlayPos", VertexFormatElement.UV1)
            /* 28 + 2*2 */.add("in_lightmap", VertexFormatElement.UV2)
            /* 32 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 35 + 1*1 */.padding(1)

            /* 36 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 40 + 1*4 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 44 + 1*2 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 46 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 47 + 1*1 */.padding(1)
            .build();

        public static final VertexFormat ENTITY_SHADOW = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_overlayPos", VertexFormatElement.UV1)
            /* 28 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .build();

        public static final VertexFormat PARTICLE = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 20 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 24 + 2*2 */.add("in_lightmap", VertexFormatElement.UV2)

            /* 28 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.padding(1)
            // .add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 32 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            // .add("MaterialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 36 + 4*1 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            .build();

        public static final VertexFormat PARTICLE_SHADOW = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 20 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 24 + 2*2 */.add("in_lightmap", VertexFormatElement.UV2)
            /* 28 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .build();

        public static final VertexFormat POSITION_COLOR_LIGHTMAP = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*2 */.add("in_lightmap", VertexFormatElement.UV2)
            .build();

        public static final VertexFormat POSITION_TEX = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            .build();

    }

}