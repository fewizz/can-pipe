package fewizz.canpipe.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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

    private static final Set<Material> allUsedMaterials = new HashSet<>();
    private static final Map<ChunkSectionLayer, Set<Material>> materialsUsedByLayer = new EnumMap<>(ChunkSectionLayer.class);

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

    public static Collection<Material> getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer layer) {
        return MaterialMaps.materialsUsedByLayer.getOrDefault(layer, Collections.emptySet());
    }

    public static Collection<Material> getAllUsedMaterials() {
        return MaterialMaps.allUsedMaterials;
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(
        PreparableReloadListener.@NonNull SharedState sharedState,
        @NonNull Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        @NonNull Executor applyExecutor
    ) {
        return CompletableFuture
            .supplyAsync(() -> MaterialMaps.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(MaterialMaps::loadRaw, applyExecutor);
    }

    public static Map<Identifier, JsonObject> readRaw(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> jsons = new LinkedHashMap<>();
        resourceManager.listResources(
            "materialmaps",
            (Identifier id) -> {
                String pathStr = id.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((id, resource) -> {
            JsonObject materialMapJson;
            try {
                materialMapJson = Jankson.builder().build().load(resource.open());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't parse material map json file \""+id+"\"", e);
                return;
            }
            id = id.withPath(id.getPath().substring("materialmaps/".length()).replace(".json5", "").replace(".json", ""));
            jsons.put(id, materialMapJson);
        });
        return jsons;
    }

    public static void loadRaw(Map<Identifier, JsonObject> materialMapsJson) {
        MaterialMaps.blocks.clear();

        MaterialMaps.blockEntities.clear();
        MaterialMaps.fluids.clear();
        MaterialMaps.items.clear();
        MaterialMaps.entities.clear();

        MaterialMaps.materialsUsedByLayer.clear();
        MaterialMaps.allUsedMaterials.clear();

        // https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/impl/model/FluidModelImpl.java#L66
        final JsonObject builtinWaterMaterialMap = new JsonObject() {{ put("defaultMaterial", new JsonPrimitive("can-pipe:water")); }};
        final JsonObject builtinLavaMaterialMap = new JsonObject() {{ put("defaultMaterial", new JsonPrimitive("can-pipe:lava")); }};

        materialMapsJson.computeIfAbsent(Identifier.parse("minecraft:fluid/water"), (Identifier id) -> builtinWaterMaterialMap);
        materialMapsJson.computeIfAbsent(Identifier.parse("minecraft:fluid/flowing_water"), (Identifier id) -> builtinWaterMaterialMap);

        materialMapsJson.computeIfAbsent(Identifier.parse("minecraft:fluid/lava"), (Identifier id) -> builtinLavaMaterialMap);
        materialMapsJson.computeIfAbsent(Identifier.parse("minecraft:fluid/flowing_lava"), (Identifier id) -> builtinLavaMaterialMap);

        for (var entry : materialMapsJson.entrySet()) {
            Identifier id = entry.getKey();

            id = CanPipe.upgradeResourcePath(id);

            int slashIdx = id.getPath().indexOf("/");
            String type = id.getPath().substring(0, slashIdx);
            id = id.withPath(id.getPath().substring(slashIdx+1));

            try {
                JsonObject materialMapJson = entry.getValue();

                if (type.equals("entity")) {
                    MaterialMap materialMap = MaterialMap.loadEntity(materialMapJson);
                    var entity = BuiltInRegistries.ENTITY_TYPE.get(id);
                    if (entity.isEmpty()) continue;
                    MaterialMaps.entities.put(entity.get().value(), materialMap);
                    MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
                    continue;
                }

                MaterialMap materialMap = MaterialMap.load(materialMapJson);

                if (type.equals("block")) {
                    var block = BuiltInRegistries.BLOCK.get(id);
                    if (block.isEmpty()) continue;
                    MaterialMaps.blocks.put(block.get().value(), materialMap);
                    var usedMaterials = materialMap.getUsedMaterials();
                    MaterialMaps.allUsedMaterials.addAll(usedMaterials);
                    var layers = MaterialMaps.getLayersUsedByBlock(block.get().value());
                    for (var layer : layers) {
                        materialsUsedByLayer.computeIfAbsent(layer, l -> new HashSet<>()).addAll(usedMaterials);
                    }
                }
                if (type.equals("block_entity")) {
                    var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
                    if (blockEntityType.isEmpty()) continue;
                    MaterialMaps.blockEntities.put(blockEntityType.get().value(), materialMap);
                    MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
                }
                if (type.equals("fluid")) {
                    System.out.println(id);
                    var fluid = BuiltInRegistries.FLUID.get(id);
                    if (fluid.isEmpty()) {
                        continue;
                    }
                    MaterialMaps.fluids.put(fluid.get().value(), materialMap);
                    var usedMaterials = materialMap.getUsedMaterials();
                    MaterialMaps.allUsedMaterials.addAll(usedMaterials);
                    var layers = MaterialMaps.getLayersUsedByFluid(fluid.get().value());
                    for (var layer : layers) {
                        materialsUsedByLayer.computeIfAbsent(layer, l -> new HashSet<>()).addAll(usedMaterials);
                    }
                }
                if (type.equals("item")) {
                    var item = BuiltInRegistries.ITEM.get(id);
                    if (item.isEmpty()) continue;
                    MaterialMaps.items.put(item.get().value(), materialMap);
                    MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
                }
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load material map \""+id+"\"", e);
            }
        }
    }

    private static Set<ChunkSectionLayer> getLayersUsedByBlock(Block block) {
        Set<ChunkSectionLayer> result = EnumSet.noneOf(ChunkSectionLayer.class);

        if (block instanceof LeavesBlock) {
            result.add(ChunkSectionLayer.CUTOUT);
        }
        else {
            Minecraft mc = Minecraft.getInstance();
            RandomSource rnd = RandomSource.create();
            List<BlockStateModelPart> output = new ArrayList<>();
            mc.getModelManager().getBlockStateModelSet().get(block.defaultBlockState()).collectParts(rnd, output);

            for (BlockStateModelPart part : output) {
                for (Direction dir : Direction.values()) {
                    for (BakedQuad quad : part.getQuads(dir)) {
                        result.add(quad.materialInfo().layer());
                    }
                }
                for (BakedQuad quad : part.getQuads(null)) {
                    result.add(quad.materialInfo().layer());
                }
            }
        }

        return result;
    }

    private static Set<ChunkSectionLayer> getLayersUsedByFluid(Fluid fluid) {
        Set<ChunkSectionLayer> result = EnumSet.noneOf(ChunkSectionLayer.class);
        Minecraft mc = Minecraft.getInstance();
        result.add(mc.getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).layer());
        return result;
    }

}
