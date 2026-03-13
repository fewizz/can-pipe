package fewizz.canpipe.material;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

final public class Materials implements PreparableReloadListener {

    public static final Materials INSTANCE = new Materials();
    private Materials() {}

    private final Map<Identifier, Material> materials = new HashMap<>();
    private final Object2IntMap<Material> id = new Object2IntOpenHashMap<>();

    public static int id(Material material) {
        return INSTANCE.id.getInt(material);
    }

    public static Material get(Identifier location) {
        return INSTANCE.materials.get(location);
    }

    public static Collection<Material> all() {
        return Collections.unmodifiableCollection(INSTANCE.materials.values());
    }

    private static Collection<Material> usedByChunkSectionLayer(ChunkSectionLayer layer) {
        List<Material> result = new ArrayList<>();
        for (var material : INSTANCE.materials.values()) {
            if (MaterialMaps.chunkLayerSectionLayersThatUseMaterial(material).contains(layer)) {
                result.add(material);
            }
        }
        return result;
    }

    private static Collection<Material> usedByMovingBlockRenderType(RenderType renderType) {
        List<Material> result = new ArrayList<>();
        for (var material : INSTANCE.materials.values()) {
            if (MaterialMaps.movingBlocksRenderTypesThatUseMaterial(material).contains(renderType)) {
                result.add(material);
            }
        }
        return result;
    }

    public static Collection<Material> usedByRenderType(RenderPipeline renderPipeline) {
        if (renderPipeline == RenderPipelines.SOLID_TERRAIN) {
            return Materials.usedByChunkSectionLayer(ChunkSectionLayer.SOLID);
        }
        if (renderPipeline == RenderPipelines.CUTOUT_TERRAIN) {
            return Materials.usedByChunkSectionLayer(ChunkSectionLayer.CUTOUT);
        }
        if (renderPipeline == RenderPipelines.TRANSLUCENT_TERRAIN) {
            return Materials.usedByChunkSectionLayer(ChunkSectionLayer.TRANSLUCENT);
        }

        if (renderPipeline == RenderPipelines.SOLID_BLOCK) {
            return Materials.usedByMovingBlockRenderType(RenderTypes.solidMovingBlock());
        }
        if (renderPipeline == RenderPipelines.CUTOUT_BLOCK) {
            return Materials.usedByMovingBlockRenderType(RenderTypes.cutoutMovingBlock());
        }
        /*if (renderPipeline == RenderPipelines.TRANSLUCENT_MOVING_BLOCK) {
            return Materials.usedByMovingBlockRenderType(RenderTypes.translucentMovingBlock());
        }*/

        if (renderPipeline.getVertexFormat() == DefaultVertexFormat.ENTITY) {
            return Materials.all();
        }

        return Collections.emptyList();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState,
        Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
                return sharedState.resourceManager().listResources(
                    "materials",
                    (Identifier rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<Identifier, Resource> materialsJson) -> {
                this.materials.clear();
                this.id.clear();

                int id = 0;
                for (var e : materialsJson.entrySet()) {
                    try {
                        Identifier fullLocation = e.getKey();
                        Identifier location = fullLocation.withPath(
                            fullLocation.getPath().substring("materials/".length())
                            .replace(".json", "").replace(".json5", "")
                        );
                        JsonObject materialJson = CanPipe.JANKSON.load(e.getValue().open());
                        Material material = Material.load(sharedState.resourceManager(), location, materialJson);
                        if (id == Short.MAX_VALUE) {
                            throw new RuntimeException("Material index exceeded "+Short.MAX_VALUE);
                        }
                        this.materials.put(location, material);
                        this.id.put(material, id);
                        ++id;
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

}
