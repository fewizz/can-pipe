package fewizz.canpipe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class Materials implements PreparableReloadListener {

    public static final Map<ResourceLocation, Material> MATERIALS = new HashMap<>();
    public static final Object2IntMap<Material> ID = new Object2IntOpenHashMap<>();

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier preparationBarrier,
        ResourceManager resourceManager,
        Executor loadExecutor,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
                return resourceManager.listResources(
                    "materials",
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, Resource> materialsJson) -> {
                MATERIALS.clear();
                ID.clear();

                int id = 0;
                for (var e : materialsJson.entrySet()) {
                    try {
                        ResourceLocation fullLocation = e.getKey();
                        ResourceLocation location = fullLocation.withPath(
                            fullLocation.getPath().substring("materials/".length())
                            .replace(".json", "").replace(".json5", "")
                        );
                        JsonObject materialJson = Mod.JANKSON.load(e.getValue().open());
                        Material material = new Material(
                            resourceManager,
                            location,
                            materialJson
                        );
                        MATERIALS.put(location, material);
                        ID.put(material, id);
                        ++id;
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

    public static int id(Material material) {
        return ID.getInt(material);
    }

}
