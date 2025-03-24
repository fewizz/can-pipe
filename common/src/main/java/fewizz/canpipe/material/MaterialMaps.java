package fewizz.canpipe.material;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

final public class MaterialMaps implements PreparableReloadListener {

    public static final MaterialMaps INSTANCE = new MaterialMaps();
    private MaterialMaps() {}

    private final Map<Fluid, MaterialMap> fluids = new HashMap<>();
    private final Map<Block, MaterialMap> blocks = new HashMap<>();

    public static MaterialMap getForBlock(Block block) {
        return INSTANCE.blocks.get(block);
    }

    public static MaterialMap getForFluid(Fluid fluid) {
        return INSTANCE.fluids.get(fluid);
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
                    "materialmaps",
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, Resource> materialMapsJson) -> {
                this.fluids.clear();
                this.blocks.clear();

                for (var e : materialMapsJson.entrySet()) {
                    ResourceLocation location = e.getKey();
                    String path = location.getPath();
                    path = path.substring("materialmaps/".length());

                    String type = path.substring(0, path.indexOf("/"));
                    String subpath = path.substring((type + "/").length());
                    subpath = subpath.replace(".json", "").replace(".json5", "");

                    try {
                        JsonObject materialMapJson = CanPipe.JANKSON.load(e.getValue().open());
                        MaterialMap materialMap = new MaterialMap(materialMapJson);

                        if (type.equals("fluid")) {
                            var fluid = BuiltInRegistries.FLUID.get(location.withPath(subpath));
                            if (fluid.isEmpty()) continue;
                            this.fluids.put(fluid.get().value(), materialMap);
                        }
                        if (type.equals("block")) {
                            if (subpath.equals("grass")) subpath = "short_grass"; // compat
                            var block = BuiltInRegistries.BLOCK.get(location.withPath(subpath));
                            if (block.isEmpty()) continue;
                            this.blocks.put(block.get().value(), materialMap);
                        }
                    } catch (IOException | SyntaxError ex) {
                        ex.printStackTrace();
                    }
                }
            },
            applyExecutor
        );
    }

}
