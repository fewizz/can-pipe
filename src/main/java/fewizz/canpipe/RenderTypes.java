package fewizz.canpipe;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

public class RenderTypes {

    public static final RenderType CANPIPE_SOLID = RenderType.create(
        "solid",
        VertexFormats.BLOCK,
        Mode.QUADS,
        0x400000,  // ~ 4 MB
        true,
        false,
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_SOLID_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setOutputState(RenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType CANPIPE_CUTOUT_MIPPED = RenderType.create(
        "cutout_mipped",
        VertexFormats.BLOCK,
        Mode.QUADS,
        0x400000,  // ~ 4 MB
        true,
        false,
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setOutputState(RenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType CANPIPE_CUTOUT = RenderType.create(
        "cutout",
        VertexFormats.BLOCK,
        Mode.QUADS,
        0xC0000,  // ~ 800 KB
        true,
        false,
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET)
            .setOutputState(RenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final Function<ResourceLocation, RenderType> CANPIPE_ENTITY_SOLID = Util.memoize((resourceLocation) -> {
        return RenderType.create(
            "entity_solid",
            VertexFormats.NEW_ENTITY,
            Mode.QUADS,
            0x600,
            true,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(RenderStateShards.SOLID_TARGET)
                .createCompositeState(true)
        );
    });

    public static final Function<ResourceLocation, RenderType> CANPIPE_ENTITY_CUTOUT = Util.memoize((resourceLocation) -> {
        return RenderType.create(
            "entity_cutout",
            VertexFormats.NEW_ENTITY,
            Mode.QUADS,
            0x600,
            true,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(RenderStateShards.SOLID_TARGET)
                .createCompositeState(true)
        );
    });

    public static final BiFunction<ResourceLocation, Boolean, RenderType> CANPIPE_ENTITY_CUTOUT_NO_CULL = Util.memoize((resourceLocation, boolean_) -> {
        return RenderType.create(
            "entity_cutout_no_cull",
            VertexFormats.NEW_ENTITY,
            Mode.QUADS,
            0x600,
            true,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(RenderStateShards.SOLID_TARGET)
                .createCompositeState(boolean_)
        );
    });

}
