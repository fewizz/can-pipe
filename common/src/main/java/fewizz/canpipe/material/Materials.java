package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Materials implements PreparableReloadListener {

    public static final Materials INSTANCE = new Materials();
    private Materials() {}

    static private final Map<Identifier, Material> materials = new HashMap<>();
    static private final Short2ObjectMap<Material> materialByIndex = new Short2ObjectOpenHashMap<>();

    public static Material get(Identifier location) {
        return Materials.materials.get(location);
    }

    public static Material get(short index) {
        return Materials.materialByIndex.get(index);
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(
        PreparableReloadListener.@NonNull SharedState sharedState,
        @NonNull Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        @NonNull Executor applyExecutor
    ) {
        return CompletableFuture
            .supplyAsync(() -> Materials.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(Materials::loadRaw, applyExecutor);
    }

    public static Map<Identifier, JsonObject> readRaw(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> jsons = new LinkedHashMap<>();
        resourceManager.listResources(
            "materials",
            (Identifier id) -> {
                String pathStr = id.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((id, resource) -> {
            JsonObject result;
            try {
                result = Jankson.builder().build().load(resource.open());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't parse material json file \""+id+"\" from pack \""+resource.sourcePackId()+"\"", e);
                return;
            }
            id = id.withPath(id.getPath().substring("materials/".length()).replace(".json5", "").replace(".json", ""));
            jsons.put(id, result);
        });
        return jsons;
    }

    public static void loadRaw(Map<Identifier, JsonObject> materialsJson) {
        Materials.materials.clear();
        Materials.materialByIndex.clear();

        int indexInt = 0;
        for (var entry : materialsJson.entrySet()) {
            if (indexInt > Short.MAX_VALUE) {
                throw new RuntimeException("Material index exceeded "+Short.MAX_VALUE);
            }

            JsonObject materialJson = entry.getValue();
            Identifier id = entry.getKey();

            try {
                Material material = Material.load((short) indexInt, id, materialJson);
                Materials.materials.put(entry.getKey(), material);
                Materials.materialByIndex.put((short) indexInt, material);
                ++indexInt;
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load material \""+id+"\"", e);
            }
        }
    }

}
