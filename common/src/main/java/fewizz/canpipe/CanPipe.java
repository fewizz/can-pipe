package fewizz.canpipe;

import java.nio.file.Path;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.resources.Identifier;

public class CanPipe {
    public static final String MOD_ID = "canpipe";

    public static final Logger LOGGER = LoggerFactory.getLogger("can-pipe");

    public static final KeyMapping PIPELINES_RELOAD_KEY = new KeyMapping(
        "canpipe.key.reloadPipelines",
        GLFW.GLFW_KEY_UNKNOWN,
        new KeyMapping.Category(Identifier.parse("canpipe:canpipe"))
    );

    private static GpuBuffer quadVertexUvBuffer;
    private static GpuBuffer[] int0to3UBOBuffers;
    private static GpuTexture whiteTexture;
    private static GpuTextureView whiteTextureView;

    public static Path getCompilationErrorsDirPath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("can-pipe-compilation-errors");
    }

    public static Path getConfigurationFilePath() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameDirectory.toPath().resolve("config/can-pipe.json");
    }

    public static void afterRendererInit() {
        GpuDevice device = RenderSystem.getDevice();

        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(CanPipe.VertexFormats.POSITION_TEX.getVertexSize() * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.QUADS, CanPipe.VertexFormats.POSITION_TEX);
            bufferBuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 0.0F, 0.0F).setUv(1.0F, 0.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(0.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                CanPipe.quadVertexUvBuffer = device.createBuffer(() -> "can-pipe quad", GpuBuffer.USAGE_VERTEX, meshData.vertexBuffer());
            }
        }

        CanPipe.int0to3UBOBuffers = new GpuBuffer[] {
            device.createBuffer(() -> "can-pipe 0", GpuBuffer.USAGE_UNIFORM, MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 0))),
            device.createBuffer(() -> "can-pipe 1", GpuBuffer.USAGE_UNIFORM, MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 1))),
            device.createBuffer(() -> "can-pipe 2", GpuBuffer.USAGE_UNIFORM, MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 2))),
            device.createBuffer(() -> "can-pipe 3", GpuBuffer.USAGE_UNIFORM, MemoryUtil.memByteBuffer(MemoryUtil.memAllocInt(1).put(0, 3)))
        };

        CanPipe.whiteTexture = device.createTexture("can-pipe white", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, TextureFormat.RGBA8, 1, 1, 1, 1);
        NativeImage whitePixel = new NativeImage(1, 1, false);
        whitePixel.setPixel(0, 0, 0xFFFFFFFF);
        device.createCommandEncoder().writeToTexture(whiteTexture, whitePixel);
        CanPipe.whiteTextureView = device.createTextureView(whiteTexture);
    }

    public static void beforeRendererClose() {
        Pipelines.setLoadedPipeline(null);
    }

    public static @NonNull GpuBuffer getQuadBuffer() { return CanPipe.quadVertexUvBuffer; }
    public static @NonNull GpuBuffer[] get0to3UBOBuffers() { return CanPipe.int0to3UBOBuffers; }
    public static @NonNull GpuTexture getWhiteTexture() { return CanPipe.whiteTexture; }
    public static @NonNull GpuTextureView getWhiteTextureView() { return CanPipe.whiteTextureView; }

    public static class VertexFormatElements {

        public static final VertexFormatElement
            MATERIAL_FLAGS = VertexFormatElement.register(7, 0, VertexFormatElement.Type.BYTE, false, 1),  // UV, because it uses vertexAttrib *I* Pointer in this case
            SPRITE_INDEX = VertexFormatElement.register(8, 0, VertexFormatElement.Type.INT, false, 1),  // UV, because it uses vertexAttrib *I* Pointer in this case
            MATERIAL_INDEX = VertexFormatElement.register(9, 0, VertexFormatElement.Type.SHORT, false, 1),
            TANGENT = VertexFormatElement.register(10, 0, VertexFormatElement.Type.BYTE, true, 4),
            AO = VertexFormatElement.register(11, 0, VertexFormatElement.Type.UBYTE, true, 1);

    }

    public static class VertexFormats {

        /* size % 4 should be == 0, for quads sorting */
        public static final VertexFormat BLOCK = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)
            /* 28 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.padding(1)

            /* 32 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 36 + 4*1 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 40 + 1*2 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 42 + 1*1 */.add("in_ao", CanPipe.VertexFormatElements.AO)
            /* 43 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            .build();

        public static final VertexFormat ENTITY = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 1*4 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_overlayPos", VertexFormatElement.UV1)
            /* 28 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)
            /* 32 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 35 + 1*1 */.padding(1)

            /* 36 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 40 + 1*4 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 44 + 1*2 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 46 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 47 + 1*1 */.padding(1)
            .build();

        /* Used when rendering shadow cascades, not a circular shadow under entities */
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
            /* 24 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)

            /* 28 + 3*1 */.add("in_normal", VertexFormatElement.NORMAL)
            /* 31 + 1*1 */.padding(1)
            /* 32 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            /* 36 + 4*1 */.add("in_tangent", CanPipe.VertexFormatElements.TANGENT)
            /* 38 + 2*1 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 40 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 41 + 1*1 */.padding(1)
            .build();

        public static final VertexFormat PARTICLE_SHADOW = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 20 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 24 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)
            /* 28 + 1*4 */.add("in_spriteIndex", CanPipe.VertexFormatElements.SPRITE_INDEX)
            .build();

        public static final VertexFormat POSITION_COLOR_LIGHTMAP = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)
            .build();

        public static final VertexFormat POSITION_COLOR_TEX_LIGHTMAP = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 12 + 4*1 */.add("in_color", VertexFormatElement.COLOR)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            /* 24 + 2*2 */.add("in_lightmapPos", VertexFormatElement.UV2)
            /* 28 + 2*1 */.add("in_materialIndex", CanPipe.VertexFormatElements.MATERIAL_INDEX)
            /* 30 + 1*1 */.add("in_materialFlags", CanPipe.VertexFormatElements.MATERIAL_FLAGS)
            /* 31 + 1*1 */.padding(1)
            .build();

        public static final VertexFormat POSITION_TEX = VertexFormat.builder()
            /* 0  + 3*4 */.add("in_vertex", VertexFormatElement.POSITION)
            /* 16 + 2*4 */.add("in_uv", VertexFormatElement.UV0)
            .build();

    }

    public static Identifier upgradeIdentifier(Identifier id) {
        if (id.getNamespace().equals("minecraft")) {
            String path = id.getPath();
            path = switch (path) {
                case "block/grass" -> "block/short_grass";

                case "textures/models/armor/chainmail_layer_1.png" -> "textures/entity/equipment/humanoid/chainmail.png";
                case "textures/models/armor/chainmail_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/chainmail.png";

                case "textures/models/armor/gold_layer_1.png" -> "textures/entity/equipment/humanoid/gold.png";
                case "textures/models/armor/gold_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/gold.png";

                case "textures/models/armor/iron_layer_1.png" -> "textures/entity/equipment/humanoid/iron.png";
                case "textures/models/armor/iron_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/iron.png";

                case "textures/models/armor/netherite_layer_1.png" -> "textures/entity/equipment/humanoid/netherite.png";
                case "textures/models/armor/netherite_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/netherite.png";

                case "textures/models/armor/leather_layer_1.png" -> "textures/entity/equipment/humanoid/leather.png";
                case "textures/models/armor/leather_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/leather.png";

                case "textures/models/armor/leather_layer_1_overlay.png" -> "textures/entity/equipment/humanoid/leather_overlay.png";
                case "textures/models/armor/leather_layer_2_overlay.png" -> "textures/entity/equipment/humanoid_leggings/leather_overlay.png";

                case "textures/models/armor/diamond_layer_1.png" -> "textures/entity/equipment/humanoid/diamond.png";
                case "textures/models/armor/diamond_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/diamond.png";

                // Was changed in resource pack format v13
                case "textures/misc/enchanted_item_glint.png" -> ItemFeatureRenderer.ENCHANTED_GLINT_ITEM.getPath();
                case "textures/misc/enchanted_glint_entity.png" -> ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR.getPath();

                // Was changed in MC 1.21.11
                case "textures/environment/sun.png" -> "textures/environment/celestial/sun.png";
                // case "textures/environment/moon_phases.png" ->  // Generated manually

                default -> path;
            };
            id = id.withPath(path);
        }
        return id;
    }

}