package fewizz.canpipe.material;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Materials implements PreparableReloadListener {

    public static final Materials INSTANCE = new Materials();
    private Materials() {}

    static private final Map<Identifier, Material> materials = new HashMap<>();

    public static Material get(Identifier location) {
        return Materials.materials.get(location);
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

    public static Map<Identifier, Resource> readRaw(ResourceManager resourceManager) {
        return resourceManager.listResources(
            "materials",
            (Identifier rl) -> {
                String pathStr = rl.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        );
    }

    public static void loadRaw(Map<Identifier, Resource> materialsJson) {
        Materials.materials.clear();

        int id = 0;
        for (var entry : materialsJson.entrySet()) {
            if (id == Short.MAX_VALUE) {
                throw new RuntimeException("Material index exceeded "+Short.MAX_VALUE);
            }

            Identifier fullLocation = entry.getKey();
            Identifier location = fullLocation.withPath(
                fullLocation.getPath().substring("materials/".length())
                .replace(".json", "").replace(".json5", "")
            );

            try {
                JsonObject materialJson = CanPipe.JANKSON.load(entry.getValue().open());
                Material material = Material.load(id, location, materialJson);
                Materials.materials.put(location, material);
                ++id;
            } catch (IOException | SyntaxError e) {
                CanPipe.LOGGER.error("Couldn't load material \""+fullLocation+"\"", e);
            }
        }
    }

}
