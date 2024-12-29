package fewizz.canpipe.pipeline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.pipeline.MainTarget;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import blue.endless.jankson.api.SyntaxError;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import fewizz.canpipe.mixininterface.GameRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class Pipelines implements PreparableReloadListener {

    private static final Map<ResourceLocation, JsonObject> RAW_PIPELINES = new HashMap<>();
    private static volatile Pipeline current = null;

    @Override
    public CompletableFuture<Void> reload(
        PreparationBarrier preparationBarrier,
        ResourceManager resourceManager,
        Executor loadExecutor,
        Executor applyExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
                return resourceManager.listResources(
                    "pipelines",
                    (ResourceLocation rl) -> {
                        String pathStr = rl.getPath();
                        return pathStr.endsWith(".json") || pathStr.endsWith(".json5");
                    }
                );
            },
            loadExecutor
        ).thenCompose(preparationBarrier::wait).thenAcceptAsync(
            (Map<ResourceLocation, Resource> jsons) -> {
                Minecraft mc = Minecraft.getInstance();

                if (current != null) {
                    current.close();
                    current = null;
                    mc.mainRenderTarget = null;
                }

                RAW_PIPELINES.clear();

                jsons.forEach((loc, pipelineRawJson) -> {
                    if (!loc.getPath().startsWith("pipelines/")) {
                        return;
                    }

                    try {
                        JsonObject o = CanPipe.JANKSON.load(pipelineRawJson.open());
                        Map<String, JsonObject> includes = new HashMap<>();
                        processIncludes(o, includes, resourceManager);
                        RAW_PIPELINES.put(loc, o);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                if (RAW_PIPELINES.size() > 0) {
                    try {
                        var raw = RAW_PIPELINES.entrySet().iterator().next();
                        current = new Pipeline(raw.getKey(), raw.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (current != null) {
                    if (mc.mainRenderTarget != null) {
                        mc.mainRenderTarget.destroyBuffers();
                    }
                    mc.mainRenderTarget = current.defaultFramebuffer;
                    ((GameRendererAccessor) mc.gameRenderer).canpipe_onPipelineActivated();
                }

                // Pipeline wasn't selected and main render target was destroyed previously
                if (mc.mainRenderTarget == null) {
                    mc.mainRenderTarget = new MainTarget(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                }
            },
            applyExecutor
        );
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
                toInclude = CanPipe.JANKSON.load(manager.open(ResourceLocation.parse(pathStr)));
                processIncludes(toInclude, includes, manager);
                includes.put(pathStr, toInclude);
            }
            JanksonUtils.mergeJsonObjectB2A(object, toInclude);
        }
    }


    public static @Nullable Pipeline getCurrent() {
        return current;
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
        final float dx0 = x2 - x1;
        final float dy0 = y2 - y1;
        final float dz0 = z2 - z1;
        final float dx1 = x0 - x1;
        final float dy1 = y0 - y1;
        final float dz1 = z0 - z1;

        float nx = dy0 * dz1 - dz0 * dy1;
        float ny = dz0 * dx1 - dx0 * dz1;
        float nz = dx0 * dy1 - dy0 * dx1;

        return new Vector3f(nx, ny, nz).normalize();
    }

}
