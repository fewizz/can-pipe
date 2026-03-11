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

    private final Map<Block, MaterialMap> blocks = new HashMap<>();
    private final Map<BlockEntityType<?>, MaterialMap> blockEntities = new HashMap<>();
    private final Map<Item, MaterialMap> items = new HashMap<>();
    private final Map<Fluid, MaterialMap> fluids = new HashMap<>();
    private final Map<EntityType<?>, MaterialMap> entities = new HashMap<>();

    public static MaterialMap getForBlock(Block block) {
        return INSTANCE.blocks.get(block);
    }

    public static MaterialMap getForBlockEntity(BlockEntityType<?> blockEntityType) {
        return INSTANCE.blockEntities.get(blockEntityType);
    }

    public static MaterialMap getForItem(Item item) {
        return INSTANCE.items.get(item);
    }

    public static MaterialMap getForFluid(Fluid fluid) {
        return INSTANCE.fluids.get(fluid);
    }

    public static MaterialMap getForEntity(EntityType<?> entityType) {
        return INSTANCE.entities.get(entityType);
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
                this.blocks.clear();
                this.blockEntities.clear();
                this.fluids.clear();
                this.items.clear();
                this.entities.clear();

                for (var e : materialMapsJson.entrySet()) {
                    Identifier location = e.getKey();
                    location = updateResourcePath(location);
                    String path = location.getPath();
                    path = path.substring("materialmaps/".length());

                    String type = path.substring(0, path.indexOf("/"));
                    String subpath = path.substring((type + "/").length());
                    subpath = subpath.replace(".json", "").replace(".json5", "");

                    try {
                        JsonObject materialMapJson = CanPipe.JANKSON.load(e.getValue().open());

                        if (type.equals("entity")) {
                            MaterialMap materialMap = MaterialMap.loadEntity(materialMapJson);
                            var entity = BuiltInRegistries.ENTITY_TYPE.get(location.withPath(subpath));
                            if (entity.isEmpty()) continue;
                            this.entities.put(entity.get().value(), materialMap);
                            continue;
                        }

                        MaterialMap materialMap = MaterialMap.load(materialMapJson);

                        if (type.equals("block")) {
                            if (subpath.equals("grass")) subpath = "short_grass";  // compat
                            var block = BuiltInRegistries.BLOCK.get(location.withPath(subpath));
                            if (block.isEmpty()) continue;
                            this.blocks.put(block.get().value(), materialMap);
                        }
                        if (type.equals("block_entity")) {
                            var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(location.withPath(subpath));
                            if (blockEntityType.isEmpty()) continue;
                            this.blockEntities.put(blockEntityType.get().value(), materialMap);
                        }
                        if (type.equals("fluid")) {
                            var fluid = BuiltInRegistries.FLUID.get(location.withPath(subpath));
                            if (fluid.isEmpty()) continue;
                            this.fluids.put(fluid.get().value(), materialMap);
                        }
                        if (type.equals("item")) {
                            var item = BuiltInRegistries.ITEM.get(location.withPath(subpath));
                            if (item.isEmpty()) continue;
                            this.items.put(item.get().value(), materialMap);
                        }
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

    public static Identifier updateResourcePath(Identifier identifier) {
        String path = identifier.getPath();

        if (identifier.getNamespace().equals("minecraft")) {
            path = switch (path) {
                case "block/grass" -> "block/short_grass";

                case "textures/models/armor/chainmail_layer_1.png" -> "textures/entity/equipment/humanoid/chainmail.png";
                case "textures/models/armor/chainmail_layer_2.png" -> "textures/entity/equipment/humanoid_leggings/chainmail.png";

                case "textures/models/armor/gold_layer_1.png" -> "textures/entity/equipment/humanoid/gold.png";
                case "textures/models/armor/gold_layer_2.png" -> "textures/entity/equipment/humanoid_legging/gold.png";

                case "textures/models/armor/iron_layer_1.png" -> "textures/entity/equipment/humanoid/iron.png";
                case "textures/models/armor/iron_layer_2.png" -> "textures/entity/equipment/humanoid_legging/iron.png";

                case "textures/models/armor/netherite_layer_1.png" -> "textures/entity/equipment/humanoid/netherite.png";
                case "textures/models/armor/netherite_layer_2.png" -> "textures/entity/equipment/humanoid_legging/netherite.png";

                case "textures/models/armor/leather_layer_1.png" -> "textures/entity/equipment/humanoid/leather.png";
                case "textures/models/armor/leather_layer_2.png" -> "textures/entity/equipment/humanoid_legging/leather.png";

                case "textures/models/armor/leather_layer_1_overlay.png" -> "textures/entity/equipment/humanoid/leather_overlay.png";
                case "textures/models/armor/leather_layer_2_overlay.png" -> "textures/entity/equipment/humanoid_legging/leather_overlay.png";

                case "textures/models/armor/diamond_layer_1.png" -> "textures/entity/equipment/humanoid/diamond.png";
                case "textures/models/armor/diamond_layer_2.png" -> "textures/entity/equipment/humanoid_legging/diamond.png";
                default -> path;
            };
            identifier = identifier.withPath(path);
        }
        return identifier;
    };

}
