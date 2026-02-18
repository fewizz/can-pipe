package fewizz.canpipe.material;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.material.Fluid;

final public class MaterialMaps implements PreparableReloadListener {

    public static final MaterialMaps INSTANCE = new MaterialMaps();
    private MaterialMaps() {}

    private final Map<Fluid, MaterialMap> fluids = new HashMap<>();
    private final Map<Block, MaterialMap> blocks = new HashMap<>();

    public static MaterialMap getForBlock(Block block) {
        return INSTANCE.blocks.get(block);
    }

    public static MaterialMap getForFluid(Fluid fluid) {
        return INSTANCE.fluids.get(fluid);
    }

    private static Stream<Block> blocksThatUseMaterial(Material material) {
        return INSTANCE.blocks.entrySet().stream()
            .filter(e -> e.getValue().usesMaterial(material))
            .map(e -> e.getKey());
    }

    private static Stream<Fluid> fluidsThatUseMaterial(Material material) {
        return INSTANCE.fluids.entrySet().stream()
            .filter(e -> e.getValue().usesMaterial(material))
            .map(e -> e.getKey());
    }

    static Set<ChunkSectionLayer> chunkLayerSectoinLayersThatUseMaterial(Material material) {
        Set<ChunkSectionLayer> result = new HashSet<>();
        blocksThatUseMaterial(material).forEach(block -> {
            if (block instanceof LeavesBlock) {
                result.add(ChunkSectionLayer.CUTOUT);
            }
            result.add(ItemBlockRenderTypes.getChunkRenderType(block.defaultBlockState()));
        });
        fluidsThatUseMaterial(material).forEach(fluid -> {
            result.add(ItemBlockRenderTypes.getRenderLayer(fluid.defaultFluidState()));
        });
        return result;
    }

    static Set<RenderType> movingBlocksRenderTypesThatUseMaterial(Material material) {
        Set<RenderType> result = new HashSet<>();
        blocksThatUseMaterial(material).forEach((block) -> {
            if (block instanceof LeavesBlock) {
                result.add(RenderTypes.cutoutMovingBlock());
            }
            result.add(ItemBlockRenderTypes.getMovingBlockRenderType(block.defaultBlockState()));
        });
        return result;
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState,
        Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                return sharedState.resourceManager().listResources(
                    "materialmaps",
                    (Identifier rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<Identifier, Resource> materialMapsJson) -> {
                this.fluids.clear();
                this.blocks.clear();

                for (var e : materialMapsJson.entrySet()) {
                    Identifier location = e.getKey();
                    String path = location.getPath();
                    path = path.substring("materialmaps/".length());

                    String type = path.substring(0, path.indexOf("/"));
                    String subpath = path.substring((type + "/").length());
                    subpath = subpath.replace(".json", "").replace(".json5", "");

                    try {
                        JsonObject materialMapJson = CanPipe.JANKSON.load(e.getValue().open());
                        MaterialMap materialMap = new MaterialMap(materialMapJson);

                        if (type.equals("fluid")) {
                            var fluid = BuiltInRegistries.FLUID.get(location.withPath(subpath));
                            if (fluid.isEmpty()) continue;
                            this.fluids.put(fluid.get().value(), materialMap);
                        }
                        if (type.equals("block")) {
                            if (subpath.equals("grass")) subpath = "short_grass";  // compat
                            var block = BuiltInRegistries.BLOCK.get(location.withPath(subpath));
                            if (block.isEmpty()) continue;
                            this.blocks.put(block.get().value(), materialMap);
                        }
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

}
