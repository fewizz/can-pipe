package fewizz.canpipe.light;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Lights implements PreparableReloadListener {

    public static final Lights INSTANCE = new Lights();
    private Lights() {}

    static private final Map<Identifier, Light> lights = new HashMap<>();

    public static Light get(Identifier location) {
        return Lights.lights.get(location);
    }

    @Override
    public @NonNull CompletableFuture<Void> reload(
        PreparableReloadListener.@NonNull SharedState sharedState,
        @NonNull Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        @NonNull Executor applyExecutor
    ) {
        return CompletableFuture
            .supplyAsync(() -> Lights.readRaw(sharedState.resourceManager()), loadExecutor)
            .thenCompose(preparationBarrier::wait)
            .thenAcceptAsync(Lights::loadRaw, applyExecutor);
    }

    public static Map<Identifier, JsonObject> readRaw(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> jsons = new LinkedHashMap<>();
        resourceManager.listResources(
            "lights/item",
            (Identifier id) -> {
                String pathStr = id.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        ).forEach((id, resource) -> {
            JsonObject result = new JsonObject();
            try {
                result = Jankson.builder().build().load(resource.open());
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't parse light json file \""+id+"\" from pack \""+resource.sourcePackId()+"\"", e);
                return;
            }
            id = id.withPath(id.getPath().substring("lights/item/".length()).replace(".json5", "").replace(".json", ""));
            jsons.put(id, result);
        });
        return jsons;
    }

    public static void loadRaw(Map<Identifier, JsonObject> lightJsons) {
        Lights.lights.clear();

        for (var entry : lightJsons.entrySet()) {
            JsonObject json = entry.getValue();
            Identifier id = entry.getKey();
            try {
                Lights.lights.put(id, new Light(json));
            } catch (Exception e) {
                CanPipe.LOGGER.error("Couldn't load light \""+id+"\"");
            }
        }
    }

}
