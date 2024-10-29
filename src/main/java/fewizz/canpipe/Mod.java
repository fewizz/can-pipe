package fewizz.canpipe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.systems.RenderSystem;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class Mod implements ClientModInitializer {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Jankson JANKSON = Jankson.builder().build();

    private static final Map<ResourceLocation, JsonObject> RAW_PIPELINES = new HashMap<>();
    private static final Map<ResourceLocation, JsonObject> RAW_MATERIALS = new HashMap<>();
    private static Pipeline currentPipeline = null;

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleResourceReloadListener<Map<ResourceLocation, Resource>>() {

                @Override
                public ResourceLocation getFabricId() {
                    return ResourceLocation.fromNamespaceAndPath(MOD_ID, "pipelines");
                }

                @Override
                public CompletableFuture<Map<ResourceLocation, Resource>> load(
                    ResourceManager manager,
                    Executor executor
                ) {
                    return CompletableFuture.supplyAsync(() -> {
                        return onResourceListenerPrepare(manager);
                    }, executor);
                }

                @Override
                public CompletableFuture<Void> apply(
                    Map<ResourceLocation, Resource> data,
                    ResourceManager manager,
                    Executor executor
                ) {
                    return CompletableFuture.runAsync(() -> {
                        onResourceListenerApply(manager, data);
                    }, executor);
                }

            }
        );

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleResourceReloadListener<Map<ResourceLocation, Resource>>() {

                @Override
                public ResourceLocation getFabricId() {
                    return ResourceLocation.fromNamespaceAndPath(MOD_ID, "materials");
                }

                @Override
                public CompletableFuture<Map<ResourceLocation, Resource>> load(
                    ResourceManager manager,
                    Executor executor
                ) {
                    return CompletableFuture.supplyAsync(() -> {
                        return manager.listResources(
                            "materials",
                            (ResourceLocation rl) -> {
                                String pathStr = rl.getPath();
                                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                            }
                        );
                    }, executor);
                }

                @Override
                public CompletableFuture<Void> apply(
                    Map<ResourceLocation, Resource> data,
                    ResourceManager manager,
                    Executor executor
                ) {
                    return CompletableFuture.runAsync(() -> {
                        
                    }, executor);
                }

            }
        );
    }

    public static @Nullable Pipeline getCurrentPipeline() {
        return currentPipeline;
    }

    private static Map<ResourceLocation, Resource> onResourceListenerPrepare(
        ResourceManager instance
    ) {
        Map<ResourceLocation, Resource> pipelines = instance.listResources(
            "pipelines",
            (ResourceLocation rl) -> {
                String pathStr = rl.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        );

        Map<ResourceLocation, Resource> materials = instance.listResources(
            "materials",
            (ResourceLocation rl) -> {
                String pathStr = rl.getPath();
                return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
            }
        );

        return new ImmutableMap.Builder<ResourceLocation, Resource>()
            .putAll(pipelines)
            .putAll(materials)
            .build();
    }

    private static void onResourceListenerApply(
        ResourceManager manager,
        Map<ResourceLocation, Resource> pipelineRawJsons
    ) {
        RenderSystem.assertOnRenderThread();

        if (currentPipeline != null) {
            currentPipeline.close();
            currentPipeline = null;
        }

        RAW_PIPELINES.clear();

        pipelineRawJsons.forEach((loc, pipelineRawJson) -> {
            if (!loc.getPath().startsWith("pipelines/")) {
                return;
            }

            try {
                JsonObject o = JANKSON.load(pipelineRawJson.open());
                Map<String, JsonObject> includes = new HashMap<>();
                processIncludes(o, includes, manager);
                RAW_PIPELINES.put(loc, o);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (RAW_PIPELINES.size() > 0) {
            try {
                var raw = RAW_PIPELINES.entrySet().iterator().next();
                currentPipeline = Pipeline.createFromData(manager, raw.getKey(), raw.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Minecraft mc = Minecraft.getInstance();

        if (currentPipeline != null) {
            mc.mainRenderTarget = currentPipeline.getDefaultFramebuffer();
        }
        else if (!(mc.mainRenderTarget instanceof MainTarget)) {
            mc.mainRenderTarget = new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
    }

    public static CompiledShaderProgram tryGetMaterialProgramReplacement(ShaderProgram shaderProgram) {
        if(currentPipeline != null) {
            return currentPipeline.materialPrograms.get(shaderProgram);
        }
        return null;
    }

    public static void onGameRendererResize(int w, int h) {
        if (currentPipeline != null) {
            currentPipeline.onWindowSizeChanged(w, h);
        }
    }

    private static void processIncludes(
        JsonObject object,
        Map<String, JsonObject> includes,
        ResourceManager manager
    ) throws IOException, SyntaxError {
        JsonArray includesA = (JsonArray) object.remove("include");
        if (includesA == null) {
            return;
        }
        for (var path : includesA) {
            JsonObject toInclude = includes.getOrDefault(path, null);
            if (toInclude == null) {
                String pathStr = ((JsonPrimitive) path).asString();
                toInclude = JANKSON.load(manager.open(ResourceLocation.parse(pathStr)));
                processIncludes(toInclude, includes, manager);
                includes.put(pathStr, toInclude);
            }
            mergeJsonObjectB2A(object, toInclude);
        }
    }

    private static void mergeJsonObjectB2A(JsonObject a, JsonObject b) {
        for (var e : b.entrySet()) {
            var k = e.getKey();
            var bv = e.getValue();
            var av = a.get(k);

            if (av != null) {
                if (bv instanceof JsonArray ba) {
                    if (av instanceof JsonArray aa) {
                        aa.addAll(ba);
                        continue;
                    }
                    throw new RuntimeException("Expected array");
                }
                if (bv instanceof JsonObject bo) {
                    if (av instanceof JsonObject ao) {
                        mergeJsonObjectB2A(ao, bo);
                        continue;
                    }
                    throw new RuntimeException("Expected object");
                }
            }

            a.put(k, bv);
        }
    }

    public static void onBeforeWorldRender(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;

        for (Pass pass : p.beforeWorldRender) {
            pass.apply(view, projection);
        }
    }

    public static void onAfterWorldRender(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;

        for (Pass pass : p.fabulous) {
            pass.apply(view, projection);
        }
    }

    public static void onAfterRenderHand(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;

        for (Pass pass : p.afterRenderHand) {
            pass.apply(view, projection);
        }
    }
}