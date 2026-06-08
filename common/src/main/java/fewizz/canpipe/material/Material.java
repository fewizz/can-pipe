package fewizz.canpipe.material;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.NonNull;


public record Material(
    short index,
    Identifier id,
    @Nullable String vertexShaderSource,
    @Nullable String fragmentShaderSource,
    @Nullable String depthVertexShaderSource,
    @Nullable String depthFragmentShaderSource,
    boolean disableAO,
    boolean disableDiffuse,
    boolean disableColorIndex,
    boolean emissive
    // Not implemented:
    // blendMode
    // preset (solid, cutout, cutout_mipped, translucent, default)
    // blur
    // cull
    // cutout (cutout_half, cutout_tenth, cutout_zero, cutout_alpha, cutout_none)
    // decal (polygon_offset, view_offset, none)
    // depthTest (always, equal, lequal, disable)
    // discardsTexture
    // flashOverlay
    // fog
    // hurtOverlay
    // lines
    // sorted
    // target (main, outline, translucent, particles, weather, clouds, entities)
    // texture
    // transparency (none, additive, lightning, glint, crumbling, translucent, default)
    // unmipped
    // writeMask (color, depth, color_depth)
    // castShadows
) {

    static Material load(
        short index,
        Identifier id,
        JsonObject materialJson
    ) throws IOException {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

        var layers = materialJson.get("layers");
        if (layers instanceof JsonArray layersArray && !layersArray.isEmpty()) {
            JanksonUtils.mergeJsonObjectB2A(materialJson, (JsonObject) layersArray.getFirst());
        }

        String vertexShaderSource = materialJson.get(String.class, "vertexSource");
        if (vertexShaderSource != null) {
            var loc = Identifier.parse(vertexShaderSource);
            var resource = resourceManager.getResource(loc);
            vertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String fragmentShaderSource = materialJson.get(String.class, "fragmentSource");
        if (fragmentShaderSource != null) {
            var loc = Identifier.parse(fragmentShaderSource);
            var resource = resourceManager.getResource(loc);
            fragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthVertexShaderSource = materialJson.get(String.class, "depthVertexSource");
        if (depthVertexShaderSource != null) {
            var loc = Identifier.parse(depthVertexShaderSource);
            var resource = resourceManager.getResource(loc);
            depthVertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthFragmentShaderSource = materialJson.get(String.class, "depthFragmentSource");
        if (depthFragmentShaderSource != null) {
            var loc = Identifier.parse(depthFragmentShaderSource);
            var resource = resourceManager.getResource(loc);
            depthFragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        boolean disableAO = materialJson.getBoolean("disableAo", false);
        boolean disableDiffuse = materialJson.getBoolean("disableDiffuse", false);
        boolean disableColorIndex = materialJson.getBoolean("disableColorIndex", false);
        boolean emissive = materialJson.getBoolean("emissive", false);

        return new Material(
            index, id,
            vertexShaderSource, fragmentShaderSource,
            depthVertexShaderSource, depthFragmentShaderSource,
            disableAO, disableDiffuse, disableColorIndex, emissive
        );
    }

    @Override
    public @NonNull String toString() {
        return this.id.toString();
    }

}
