package fewizz.canpipe.material;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;

final public class MaterialMaps implements PreparableReloadListener {

    public static final MaterialMaps INSTANCE = new MaterialMaps();
    private MaterialMaps() {}

    private static final Map<Block, MaterialMap> blocks = new HashMap<>();
    private static final Map<BlockEntityType<?>, MaterialMap> blockEntities = new HashMap<>();
    private static final Map<Item, MaterialMap> items = new HashMap<>();
    private static final Map<Fluid, MaterialMap> fluids = new HashMap<>();
    private static final Map<EntityType<?>, MaterialMap> entities = new HashMap<>();

    public static MaterialMap getForBlock(Block block) {
        return MaterialMaps.blocks.get(block);
    }

    public static MaterialMap getForBlockEntity(BlockEntityType<?> blockEntityType) {
        return MaterialMaps.blockEntities.get(blockEntityType);
    }

    public static MaterialMap getForItem(Item item) {
        return MaterialMaps.items.get(item);
    }

    public static MaterialMap getForFluid(Fluid fluid) {
        return MaterialMaps.fluids.get(fluid);
    }

    public static MaterialMap getForEntity(EntityType<?> entityType) {
        return MaterialMaps.entities.get(entityType);
    }

    private static Stream<Block> blocksThatUseMaterial(Material material) {
        return MaterialMaps.blocks.entrySet().stream()
            .filter(e -> e.getValue().usesMaterial(material))
            .map(e -> e.getKey());
    }

    private static Stream<Fluid> fluidsThatUseMaterial(Material material) {
        return MaterialMaps.fluids.entrySet().stream()
            .filter(e -> e.getValue().usesMaterial(material))
            .map(e -> e.getKey());
    }

    static Set<ChunkSectionLayer> chunkLayerSectionLayersThatUseMaterial(Material material) {
        Set<ChunkSectionLayer> result = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        RandomSource rnd = RandomSource.create();

        blocksThatUseMaterial(material).forEach(block -> {
            if (block instanceof LeavesBlock) {
                result.add(ChunkSectionLayer.CUTOUT);
                return;
            }

            List<BlockStateModelPart> output = new ArrayList<>();
            mc.getModelManager().getBlockStateModelSet().get(block.defaultBlockState()).collectParts(rnd, output);

            for (BlockStateModelPart part : output) {
                for (Direction dir : Direction.values()) {
                    for (BakedQuad quad : part.getQuads(dir)) {
                        result.add(quad.materialInfo().layer());
                    }
                }
            }
        });
        fluidsThatUseMaterial(material).forEach(fluid -> {
            result.add(mc.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).layer());
        });
        return result;
    }

    static Set<RenderType> movingBlocksRenderTypesThatUseMaterial(Material material) {
        Set<RenderType> result = new HashSet<>();
        blocksThatUseMaterial(material).forEach((block) -> {
            if (block instanceof LeavesBlock) {
                result.add(RenderTypes.cutoutMovingBlock());
            }
            else {
                // result.add(ItemBlockRenderTypes.getMovingBlockRenderType(block.defaultBlockState())); TODO
                // result.add(ItemBlockRenderTypes.getRenderType(ChunkSectionLayer.SOLID));
                result.add(RenderTypes.solidMovingBlock());
            }
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
        return CompletableFuture
            .supplyAsync(() -> MaterialMaps.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync((Map<Identifier, Resource> materialMapsJson) -> MaterialMaps.loadRaw(materialMapsJson), applyExecutor);
    }

    public static Map<Identifier, Resource> readRaw(ResourceManager resourceManager) {
        return resourceManager.listResources(
            "materialmaps",
            (Identifier rl) -> {
                String pathStr = rl.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        );
    }

    public static void loadRaw(Map<Identifier, Resource> materialMapsJson) {
        MaterialMaps.blocks.clear();
        MaterialMaps.blockEntities.clear();
        MaterialMaps.fluids.clear();
        MaterialMaps.items.clear();
        MaterialMaps.entities.clear();

        for (var entry : materialMapsJson.entrySet()) {
            Identifier materialMapId = entry.getKey();
            Identifier elementId = materialMapId.withPath(
                materialMapId.getPath()
                .substring("materialmaps/".length())
                .replace(".json", "").replace(".json5", "")
            );

            elementId = CanPipe.upgradeResourcePath(elementId);

            int slashIdx = elementId.getPath().indexOf("/");
            String type = elementId.getPath().substring(0, slashIdx);
            elementId = elementId.withPath(elementId.getPath().substring(slashIdx+1));

            try {
                JsonObject materialMapJson = CanPipe.JANKSON.load(entry.getValue().open());

                if (type.equals("entity")) {
                    MaterialMap materialMap = MaterialMap.loadEntity(materialMapJson);
                    var entity = BuiltInRegistries.ENTITY_TYPE.get(elementId);
                    if (entity.isEmpty()) continue;
                    MaterialMaps.entities.put(entity.get().value(), materialMap);
                    continue;
                }

                MaterialMap materialMap = MaterialMap.load(materialMapJson);

                if (type.equals("block")) {
                    var block = BuiltInRegistries.BLOCK.get(elementId);
                    if (block.isEmpty()) continue;
                    MaterialMaps.blocks.put(block.get().value(), materialMap);
                }
                if (type.equals("block_entity")) {
                    var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(elementId);
                    if (blockEntityType.isEmpty()) continue;
                    MaterialMaps.blockEntities.put(blockEntityType.get().value(), materialMap);
                }
                if (type.equals("fluid")) {
                    var fluid = BuiltInRegistries.FLUID.get(elementId);
                    if (fluid.isEmpty()) continue;
                    MaterialMaps.fluids.put(fluid.get().value(), materialMap);
                }
                if (type.equals("item")) {
                    var item = BuiltInRegistries.ITEM.get(elementId);
                    if (item.isEmpty()) continue;
                    MaterialMaps.items.put(item.get().value(), materialMap);
                }
            } catch (IOException | SyntaxError e) {
                CanPipe.LOGGER.error("Couldn't load material map \""+materialMapId+"\"", e);
            }
        }
    }

}
