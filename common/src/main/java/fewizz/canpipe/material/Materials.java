package fewizz.canpipe.material;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;

final public class Materials implements PreparableReloadListener {

    public static final Materials INSTANCE = new Materials();
    private Materials() {}

    public final Map<Identifier, Material> materials = new HashMap<>();
    public final Object2IntMap<Material> id = new Object2IntOpenHashMap<>();

    public static int id(Material material) {
        return INSTANCE.id.getInt(material);
    }

    public static Material get(Identifier location) {
        return INSTANCE.materials.get(location);
    }

    public static Collection<Material> allCopy() {
        return Collections.unmodifiableCollection(INSTANCE.materials.values());
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState sharedState,
        Executor loadExecutor,
        PreparableReloadListener.PreparationBarrier preparationBarrier,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
                return sharedState.resourceManager().listResources(
                    "materials",
                    (Identifier rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<Identifier, Resource> materialsJson) -> {
                this.materials.clear();
                this.id.clear();

                int id = 0;
                for (var e : materialsJson.entrySet()) {
                    try {
                        Identifier fullLocation = e.getKey();
                        Identifier location = fullLocation.withPath(
                            fullLocation.getPath().substring("materials/".length())
                            .replace(".json", "").replace(".json5", "")
                        );
                        JsonObject materialJson = CanPipe.JANKSON.load(e.getValue().open());
                        Material material = new Material(
                            sharedState.resourceManager(),
                            location,
                            materialJson
                        );
                        if (id == Short.MAX_VALUE) {
                            throw new RuntimeException("Material index exceeded "+Short.MAX_VALUE);
                        }
                        this.materials.put(location, material);
                        this.id.put(material, id);
                        ++id;
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

}
