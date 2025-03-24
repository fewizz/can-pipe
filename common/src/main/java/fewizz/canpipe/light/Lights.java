package fewizz.canpipe.light;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final public class Lights implements PreparableReloadListener {

    public static final Lights INSTANCE = new Lights();
    private Lights() {}

    private final Map<ResourceLocation, Light> lights = new HashMap<>();

    public static Light get(ResourceLocation location) {
        return INSTANCE.lights.get(location);
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier preparationBarrier,
        ResourceManager resourceManager,
        Executor loadExecutor,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                return resourceManager.listResources(
                    "lights/item",
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, Resource> lightJsons) -> {
                lights.clear();

                for (var e : lightJsons.entrySet()) {
                    ResourceLocation fullLocation = e.getKey();
                    ResourceLocation location = fullLocation.withPath(
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
