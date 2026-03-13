package fewizz.canpipe.light;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Lights implements PreparableReloadListener {

    public static final Lights INSTANCE = new Lights();
    private Lights() {}

    static private final Map<Identifier, Light> lights = new HashMap<>();

    public static Light get(Identifier location) {
        return Lights.lights.get(location);
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState,
        Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor applyExecutor
    ) {
        return CompletableFuture
            .supplyAsync(() -> Lights.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync((Map<Identifier, Resource> lightJsons) -> Lights.loadRaw(lightJsons), applyExecutor);
    }

    public static Map<Identifier, Resource> readRaw(ResourceManager resourceManager) {
        return resourceManager.listResources(
            "lights/item",
            (Identifier rl) -> {
                String pathStr = rl.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        );
    }

    public static void loadRaw(Map<Identifier, Resource> lightJsons) {
        Lights.lights.clear();

        for (var entry : lightJsons.entrySet()) {
            Identifier fullLocation = entry.getKey();
            Identifier location = fullLocation.withPath(
                fullLocation.getPath().substring("lights/item/".length())
                .replace(".json", "").replace(".json5", "")
            );

            try {
                JsonObject json = CanPipe.JANKSON.load(entry.getValue().open());
                Lights.lights.put(location, new Light(json));
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load light \""+location+"\"");
            }
        }
    }

}
