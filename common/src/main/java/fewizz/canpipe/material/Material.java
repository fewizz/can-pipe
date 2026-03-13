package fewizz.canpipe.material;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public record Material(
    Identifier location,
    @Nullable String vertexShaderSource,
    @Nullable String fragmentShaderSource,
    @Nullable String depthVertexShaderSource,
    @Nullable String depthFragmentShaderSource,
    boolean disableAO,
    boolean disableDiffuse
) {

    public static Material load(
        ResourceManager manager,
        Identifier location,
        JsonObject materialJson
    ) throws FileNotFoundException, IOException {
        var layers = materialJson.get("layers");
        if (layers instanceof JsonArray layersArray && layersArray.size() > 0) {
            JanksonUtils.mergeJsonObjectB2A(materialJson, (JsonObject) layersArray.get(0));
        }

        String vertexShaderSource = materialJson.get(String.class, "vertexSource");
        if (vertexShaderSource != null) {
            var loc = Identifier.parse(vertexShaderSource);
            var resource = manager.getResource(loc);
            vertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String fragmentShaderSource = materialJson.get(String.class, "fragmentSource");
        if (fragmentShaderSource != null) {
            var loc = Identifier.parse(fragmentShaderSource);
            var resource = manager.getResource(loc);
            fragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthVertexShaderSource = materialJson.get(String.class, "depthVertexSource");
        if (depthVertexShaderSource != null) {
            var loc = Identifier.parse(depthVertexShaderSource);
            var resource = manager.getResource(loc);
            depthVertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthFragmentShaderSource = materialJson.get(String.class, "depthFragmentSource");
        if (depthFragmentShaderSource != null) {
            var loc = Identifier.parse(depthFragmentShaderSource);
            var resource = manager.getResource(loc);
            depthFragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        boolean disableAO = materialJson.getBoolean("disableAo", false);
        boolean disableDiffuse = materialJson.getBoolean("disableDiffuse", false);

        return new Material(
            location,
            vertexShaderSource, fragmentShaderSource,
            depthVertexShaderSource, depthFragmentShaderSource,
            disableAO, disableDiffuse
        );
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

}
