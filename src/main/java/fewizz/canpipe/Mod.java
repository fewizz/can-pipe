package fewizz.canpipe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
import fewizz.canpipe.pipeline.Pipeline;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class Mod implements ClientModInitializer {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Jankson JANKSON = Jankson.builder().build();

    private static final Map<ResourceLocation, JsonObject> RAW_PIPELINES = new HashMap<>();
    /*private static final Map<ResourceLocation, JsonObject> RAW_MATERIALS = new HashMap<>();*/

    private static final Map<ResourceLocation, Material> MATERIALS = new HashMap<>();
    private static volatile Pipeline currentPipeline = null;

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
                        MATERIALS.clear();
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
                currentPipeline = new Pipeline(manager, raw.getKey(), raw.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Minecraft mc = Minecraft.getInstance();

        if (currentPipeline != null) {
            mc.mainRenderTarget = currentPipeline.defaultFramebuffer;
        }
        else if (!(mc.mainRenderTarget instanceof MainTarget)) {
            mc.mainRenderTarget = new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());
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

    public static void onGameRendererResize(int w, int h) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;
        p.onWindowSizeChanged(w, h);
    }

    public static void onBeforeWorldRender(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;
        p.onBeforeWorldRender(view, projection);
    }

    public static void onAfterWorldRender(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;
        p.onAfterWorldRender(view, projection);
    }

    public static void onAfterRenderHand(Matrix4f view, Matrix4f projection) {
        Pipeline p = getCurrentPipeline();
        if (p == null) return;
        p.onAfterRenderHand(view, projection);
    }

    public static Vector3f computeTangent(
        float x0, float y0, float z0, float u0, float v0,
        float x1, float y1, float z1, float u1, float v1,
        float x2, float y2, float z2, float u2, float v2
    ) {
        // taken from frex
        final float dv0 = v1 - v0;
        final float dv1 = v2 - v1;
        final float du0 = u1 - u0;
        final float du1 = u2 - u1;
        final float inverseLength = 1.0f / (du0 * dv1 - du1 * dv0);

        final float tx = inverseLength * (dv1 * (x1 - x0) - dv0 * (x2 - x1));
        final float ty = inverseLength * (dv1 * (y1 - y0) - dv0 * (y2 - y1));
        final float tz = inverseLength * (dv1 * (z1 - z0) - dv0 * (z2 - z1));

        // TODO
        // final float bx = inverseLength * (-du1 * (x1 - x(0)) + du0 * (x(2) - x1));
        // final float by = inverseLength * (-du1 * (y1 - y(0)) + du0 * (y(2) - y1));
        // final float bz = inverseLength * (-du1 * (z1 - z(0)) + du0 * (z(2) - z1));

        // Compute handedness
        // final float nx = this.normalX(0);
        // final float ny = this.normalY(0);
        // final float nz = this.normalZ(0);

        // T cross N
        // final float TcNx = ty * nz - tz * ny;
        // final float TcNy = tz * nx - tx * nz;
        // final float TcNz = tx * ny - ty * nx;

        // B dot TcN
        // final float BdotTcN = bx * TcNx + by * TcNy + bz * TcNz;
        // final boolean inverted = BdotTcN < 0f;

        return new Vector3f(tx, ty, tz);
    }

    public static Vector3f computeNormal(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2
    ) {
        final float dx0 = x0 - x1;
        final float dy0 = y0 - y1;
        final float dz0 = z0 - z1;
        final float dx1 = x2 - x1;
        final float dy1 = y2 - y1;
        final float dz1 = z2 - z1;

        float nx = dy0 * dz1 - dz0 * dy1;
        float ny = dz0 * dx1 - dx0 * dz1;
        float nz = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= l;
        ny /= l;
        nz /= l;

        return new Vector3f(nx, ny, nz);
    }

}