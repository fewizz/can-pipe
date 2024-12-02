package fewizz.canpipe;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;

public class CanPipeRenderTypes {

    public static RenderType replaced(RenderType rt) {
        if (rt == RenderType.SOLID) { return SOLID; }
        if (rt == RenderType.CUTOUT_MIPPED) { return CUTOUT_MIPPED; }
        if (rt == RenderType.CUTOUT) { return CUTOUT; }
        if (rt == RenderType.TRANSLUCENT) { return TRANSLUCENT; }

        return rt;
    }

    public static RenderType unreplaced(RenderType rt) {
        if (rt == SOLID) { return RenderType.SOLID; }
        if (rt == CUTOUT_MIPPED) { return RenderType.CUTOUT_MIPPED; }
        if (rt == CUTOUT) { return RenderType.CUTOUT; }
        if (rt == TRANSLUCENT) { return RenderType.TRANSLUCENT; }

        return rt;
    }

    public static final RenderType SOLID = RenderType.create(
        "solid",
        CanPipeVertexFormats.BLOCK,
        RenderType.SOLID.mode(),
        RenderType.SOLID.bufferSize(),
        RenderType.SOLID.affectsCrumbling(),
        RenderType.SOLID.sortOnUpload(),
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_SOLID_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType CUTOUT_MIPPED = RenderType.create(
        "cutout_mipped",
        CanPipeVertexFormats.BLOCK,
        RenderType.CUTOUT_MIPPED.mode(),
        RenderType.CUTOUT_MIPPED.bufferSize(),
        RenderType.CUTOUT_MIPPED.affectsCrumbling(),
        RenderType.CUTOUT_MIPPED.sortOnUpload(),
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType CUTOUT = RenderType.create(
        "cutout",
        CanPipeVertexFormats.BLOCK,
        RenderType.CUTOUT.mode(),
        RenderType.CUTOUT.bufferSize(),
        RenderType.CUTOUT.affectsCrumbling(),
        RenderType.CUTOUT.sortOnUpload(),
        RenderType.CompositeState.builder()
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
            .setTextureState(RenderStateShard.BLOCK_SHEET)
            .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType TRANSLUCENT = RenderType.create(
        "translucent",
        CanPipeVertexFormats.BLOCK,
        RenderType.TRANSLUCENT.mode(),
        RenderType.TRANSLUCENT.bufferSize(),
        RenderType.TRANSLUCENT.affectsCrumbling(),
        RenderType.TRANSLUCENT.sortOnUpload(),
        RenderType.translucentState(RenderType.RENDERTYPE_TRANSLUCENT_SHADER)
    );

    public static final Function<ResourceLocation, RenderType> ENTITY_SOLID = Util.memoize((resourceLocation) -> {
        return RenderType.create(
            "entity_solid",
            CanPipeVertexFormats.NEW_ENTITY,
            RenderType.ENTITY_SOLID.apply(resourceLocation).mode(),
            RenderType.ENTITY_SOLID.apply(resourceLocation).bufferSize(),
            RenderType.ENTITY_SOLID.apply(resourceLocation).affectsCrumbling(),
            RenderType.ENTITY_SOLID.apply(resourceLocation).sortOnUpload(),
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
                .createCompositeState(true)
        );
    });

    public static final Function<ResourceLocation, RenderType> ENTITY_CUTOUT = Util.memoize((resourceLocation) -> {
        return RenderType.create(
            "entity_cutout",
            CanPipeVertexFormats.NEW_ENTITY,
            RenderType.ENTITY_CUTOUT.apply(resourceLocation).mode(),
            RenderType.ENTITY_CUTOUT.apply(resourceLocation).bufferSize(),
            RenderType.ENTITY_CUTOUT.apply(resourceLocation).affectsCrumbling(),
            RenderType.ENTITY_CUTOUT.apply(resourceLocation).sortOnUpload(),
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
                .createCompositeState(true)
        );
    });

    public static final BiFunction<ResourceLocation, Boolean, RenderType> ENTITY_CUTOUT_NO_CULL = Util.memoize((resourceLocation, b) -> {
        return RenderType.create(
            "entity_cutout_no_cull",
            CanPipeVertexFormats.NEW_ENTITY,
            RenderType.ENTITY_CUTOUT_NO_CULL.apply(resourceLocation, b).mode(),
            RenderType.ENTITY_CUTOUT_NO_CULL.apply(resourceLocation, b).bufferSize(),
            RenderType.ENTITY_CUTOUT_NO_CULL.apply(resourceLocation, b).affectsCrumbling(),
            RenderType.ENTITY_CUTOUT_NO_CULL.apply(resourceLocation, b).sortOnUpload(),
            RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, TriState.FALSE, false))
                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                .setCullState(RenderStateShard.NO_CULL)
                .setLightmapState(RenderStateShard.LIGHTMAP)
                .setOverlayState(RenderStateShard.OVERLAY)
                .setOutputState(CanPipeRenderStateShards.SOLID_TARGET)
                .createCompositeState(b)
        );
    });

    public static final List<RenderType> chunkBufferLayers() {
        return RenderType.chunkBufferLayers().stream().map(
            rt -> Pipelines.getCurrent() != null ? replaced(rt) : rt
        ).toList();
    }

}
