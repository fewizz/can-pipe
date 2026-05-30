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
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
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
    private static final Map<BlockEntityType<?>, EntityMaterialMap> blockEntities = new HashMap<>();
    private static final Map<Item, MaterialMap> items = new HashMap<>();
    private static final Map<Fluid, MaterialMap> fluids = new HashMap<>();
    private static final Map<EntityType<?>, EntityMaterialMap> entities = new HashMap<>();
    private static final Map<ParticleType<?>, MaterialMap> particles = new HashMap<>();

    private static final Set<Material> allUsedMaterials = new HashSet<>();
    private static final Set<Material> materialsUsedByParticles = new HashSet<>();
    private static final Map<ChunkSectionLayer, Set<Material>> materialsUsedByLayer = new EnumMap<>(ChunkSectionLayer.class);

    public static MaterialMap getForBlock(Block block) {
        return MaterialMaps.blocks.get(block);
    }

    public static EntityMaterialMap getForBlockEntity(BlockEntityType<?> blockEntityType) {
        return MaterialMaps.blockEntities.get(blockEntityType);
    }

    public static MaterialMap getForItem(Item item) {
        return MaterialMaps.items.get(item);
    }

    public static MaterialMap getForFluid(Fluid fluid) {
        return MaterialMaps.fluids.get(fluid);
    }

    public static EntityMaterialMap getForEntity(EntityType<?> entityType) {
        return MaterialMaps.entities.get(entityType);
    }

    public static MaterialMap getForParticle(ParticleType<?> particleType) {
        return MaterialMaps.particles.get(particleType);
    }

    public static Collection<Material> getMaterialsUsedByChunkSectionLayer(ChunkSectionLayer layer) {
        return MaterialMaps.materialsUsedByLayer.getOrDefault(layer, Collections.emptySet());
    }

    public static Collection<Material> getAllUsedMaterials() {
        return MaterialMaps.allUsedMaterials;
    }

    public static Collection<Material> getMaterialsUsedByParticles() {
        return MaterialMaps.materialsUsedByParticles;
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

    record MaterialMapsJsons(
        Map<Identifier, List<JsonObject>> blockEntities,
        Map<Identifier, List<JsonObject>> entities,
        Map<Identifier, JsonObject> blocks,
        Map<Identifier, JsonObject> fluids,
        Map<Identifier, JsonObject> items,
        Map<Identifier, JsonObject> particles
    ) {}

    public static MaterialMapsJsons readRaw(ResourceManager resourceManager) {
        MaterialMapsJsons allJsons = new MaterialMapsJsons(
            new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
            new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>()
        );

        resourceManager.listResourceStacks(
            "materialmaps",
            (Identifier id) -> {
                String pathStr = id.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((id, resources) -> {
            Identifier idNormalized;
            String type;

            {
                Identifier _idNormalized = id.withPath(id.getPath().substring("materialmaps/".length()).replace(".json5", "").replace(".json", ""));
                _idNormalized = CanPipe.upgradeResourcePath(_idNormalized);

                int slashIdx = _idNormalized.getPath().indexOf("/");
                type = _idNormalized.getPath().substring(0, slashIdx);

                idNormalized = _idNormalized.withPath(_idNormalized.getPath().substring(slashIdx+1));
            }

            Consumer<Map<Identifier, JsonObject>> parse = (Map<Identifier, JsonObject> jsons) -> {
                Resource resource = resources.getLast();
                JsonObject result = new JsonObject();
                try {
                    result = Jankson.builder().build().load(resource.open());
                } catch (Exception e) {
                    CanPipe.LOGGER.error("Couldn't parse material map json file \""+id+"\" from pack \""+resource.sourcePackId()+"\"", e);
                    return;
                }
                jsons.put(idNormalized, result);
            };

            Consumer<Map<Identifier, List<JsonObject>>> parseStacked = (Map<Identifier, List<JsonObject>> jsons) -> {
                List<JsonObject> result = new ArrayList<JsonObject>();
                for (var resource : resources) {
                    try {
                        result.add(Jankson.builder().build().load(resource.open()));
                    } catch (Exception e) {
                        CanPipe.LOGGER.error("Couldn't parse material map json file \""+id+"\" from pack \""+resource.sourcePackId()+"\"", e);
                    }
                }
                if (result.isEmpty()) {
                    return;
                }
                jsons.put(idNormalized, result);
            };

            switch (type) {
                // entity and block entity material maps are stacked between packs
                case "block_entity":  // https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/impl/material/map/MaterialMapLoader.java#L207
                    parseStacked.accept(allJsons.blockEntities); break;
                case "entity":  // https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/impl/material/map/MaterialMapLoader.java#L222
                    parseStacked.accept(allJsons.entities); break;

                case "particle":
                    parse.accept(allJsons.particles); break;
                case "block":
                    parse.accept(allJsons.blocks); break;
                case "fluid":
                    parse.accept(allJsons.fluids); break;
                case "item":
                    parse.accept(allJsons.items); break;

                default:
                    break;
            }
        });

        return allJsons;
    }

    public static void loadRaw(MaterialMapsJsons allJsons) {
        MaterialMaps.blocks.clear();
        MaterialMaps.blockEntities.clear();
        MaterialMaps.fluids.clear();
        MaterialMaps.items.clear();
        MaterialMaps.entities.clear();
        MaterialMaps.particles.clear();

        MaterialMaps.materialsUsedByLayer.clear();
        MaterialMaps.materialsUsedByParticles.clear();
        MaterialMaps.allUsedMaterials.clear();

        // https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/impl/model/FluidModelImpl.java#L66
        final JsonObject builtinWaterMaterialMap = new JsonObject() {{ put("defaultMaterial", new JsonPrimitive("can-pipe:water")); }};
        final JsonObject builtinLavaMaterialMap = new JsonObject() {{ put("defaultMaterial", new JsonPrimitive("can-pipe:lava")); }};

        allJsons.fluids.computeIfAbsent(Identifier.parse("minecraft:water"), (Identifier id) -> builtinWaterMaterialMap);
        allJsons.fluids.computeIfAbsent(Identifier.parse("minecraft:flowing_water"), (Identifier id) -> builtinWaterMaterialMap);

        allJsons.fluids.computeIfAbsent(Identifier.parse("minecraft:lava"), (Identifier id) -> builtinLavaMaterialMap);
        allJsons.fluids.computeIfAbsent(Identifier.parse("minecraft:flowing_lava"), (Identifier id) -> builtinLavaMaterialMap);

        for (var entry : allJsons.blockEntities.entrySet()) {
            try {
                var blockEntityType = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(entry.getKey());
                if (blockEntityType.isEmpty()) continue;
                EntityMaterialMap materialMap = EntityMaterialMap.load(entry.getKey(), entry.getValue());
                MaterialMaps.blockEntities.put(blockEntityType.get().value(), materialMap);
                MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load block entity material map \""+entry.getKey()+"\"", e);
            }
        }

        for (var entry : allJsons.entities.entrySet()) {
            try {
                var entity = BuiltInRegistries.ENTITY_TYPE.get(entry.getKey());
                if (entity.isEmpty()) continue;
                EntityMaterialMap materialMap = EntityMaterialMap.load(entry.getKey(), entry.getValue());
                MaterialMaps.entities.put(entity.get().value(), materialMap);
                MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load entity material map \""+entry.getKey()+"\"", e);
            }
        }

        for (var entry : allJsons.particles.entrySet()) {
            try {
                var particle = BuiltInRegistries.PARTICLE_TYPE.get(entry.getKey());
                if (particle.isEmpty()) continue;
                MaterialMap materialMap = MaterialMap.loadParticle(entry.getValue());
                if (materialMap == null) continue;
                var usedMaterials = materialMap.getUsedMaterials();
                MaterialMaps.allUsedMaterials.addAll(usedMaterials);
                MaterialMaps.materialsUsedByParticles.addAll(usedMaterials);
                MaterialMaps.particles.put(particle.get().value(), materialMap);
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load particle material map \""+entry.getKey()+"\"", e);
            }
        }

        for (var entry : allJsons.blocks.entrySet()) {
            try {
                var block = BuiltInRegistries.BLOCK.get(entry.getKey());
                if (block.isEmpty()) continue;
                MaterialMap materialMap = MaterialMap.load(entry.getValue());
                MaterialMaps.blocks.put(block.get().value(), materialMap);
                var usedMaterials = materialMap.getUsedMaterials();
                MaterialMaps.allUsedMaterials.addAll(usedMaterials);
                var layers = MaterialMaps.getLayersUsedByBlock(block.get().value());
                for (var layer : layers) {
                    materialsUsedByLayer.computeIfAbsent(layer, l -> new HashSet<>()).addAll(usedMaterials);
                }
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load block material map \""+entry.getKey()+"\"", e);
            }
        }

        for (var entry : allJsons.fluids.entrySet()) {
            try {
                var fluid = BuiltInRegistries.FLUID.get(entry.getKey());
                if (fluid.isEmpty()) {continue;}
                MaterialMap materialMap = MaterialMap.load(entry.getValue());
                MaterialMaps.fluids.put(fluid.get().value(), materialMap);
                var usedMaterials = materialMap.getUsedMaterials();
                MaterialMaps.allUsedMaterials.addAll(usedMaterials);
                var layers = MaterialMaps.getLayersUsedByFluid(fluid.get().value());
                for (var layer : layers) {
                    materialsUsedByLayer.computeIfAbsent(layer, l -> new HashSet<>()).addAll(usedMaterials);
                }
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load fluid material map \""+entry.getKey()+"\"", e);
            }
        }

        for (var entry : allJsons.items.entrySet()) {
            try {
                var item = BuiltInRegistries.ITEM.get(entry.getKey());
                if (item.isEmpty()) continue;
                MaterialMap materialMap = MaterialMap.load(entry.getValue());
                MaterialMaps.items.put(item.get().value(), materialMap);
                MaterialMaps.allUsedMaterials.addAll(materialMap.getUsedMaterials());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load item material map \""+entry.getKey()+"\"", e);
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
