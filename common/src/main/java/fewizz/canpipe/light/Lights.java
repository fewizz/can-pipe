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

final public class Lights implements PreparableReloadListener {

    public static final Lights INSTANCE = new Lights();
    private Lights() {}

    private final Map<Identifier, Light> lights = new HashMap<>();

    public static Light get(Identifier location) {
        return INSTANCE.lights.get(location);
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
                    "lights/item",
                    (Identifier rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<Identifier, Resource> lightJsons) -> {
                lights.clear();

                for (var e : lightJsons.entrySet()) {
                    Identifier fullLocation = e.getKey();
                    Identifier location = fullLocation.withPath(
                        fullLocation.getPath().substring("lights/item/".length())
                        .replace(".json", "").replace(".json5", "")
                    );

                    try {
                        JsonObject json = CanPipe.JANKSON.load(e.getValue().open());
                        lights.put(location, new Light(json));
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

}
