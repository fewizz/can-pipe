package fewizz.canpipe.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Materials implements PreparableReloadListener {

    public static final Materials INSTANCE = new Materials();
    private Materials() {}

    static private final Map<Identifier, Material> materials = new HashMap<>();
    static private final List<Material> materialByIndex = new ArrayList<>();

    public static @Nullable Material get(Identifier location) {
        return Materials.materials.get(location);
    }

    public static Material get(short index) {
        if (index >= Materials.materialByIndex.size()) {
            throw new IllegalArgumentException("Requested material index ("+index+") is >= than materials count ("+Materials.materialByIndex.size());
        }
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

        for (var entry : materialsJson.entrySet()) {
            int index = Materials.materialByIndex.size();
            if (index > Short.MAX_VALUE) {
                throw new RuntimeException("Material index exceeded "+Short.MAX_VALUE);
            }

            JsonObject materialJson = entry.getValue();
            Identifier id = entry.getKey();

            try {
                Material material = Material.load((short) index, id, materialJson);
                Materials.materials.put(entry.getKey(), material);
                Materials.materialByIndex.add(material);
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load material \""+id+"\"", e);
            }
        }
    }

}
