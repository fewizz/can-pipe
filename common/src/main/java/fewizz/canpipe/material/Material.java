package fewizz.canpipe.material;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class Material {

    public final ResourceLocation location;
    @Nullable public final String vertexShaderSource;
    @Nullable public final String fragmentShaderSource;
    @Nullable public final String depthVertexShaderSource;
    @Nullable public final String depthFragmentShaderSource;

    Material(
        ResourceManager manager,
        ResourceLocation location,
        JsonObject materialJson
    ) throws FileNotFoundException, IOException {
        this.location = location;

        var layers = materialJson.get("layers");
        if (layers instanceof JsonArray layersArray && layersArray.size() > 0) {
            JanksonUtils.mergeJsonObjectB2A(materialJson, (JsonObject) layersArray.get(0));
        }

        String vertexShaderSource = materialJson.get(String.class, "vertexSource");
        if (vertexShaderSource != null) {
            var loc = ResourceLocation.parse(vertexShaderSource);
            var resource = manager.getResource(loc);
            vertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String fragmentShaderSource = materialJson.get(String.class, "fragmentSource");
        if (fragmentShaderSource != null) {
            var loc = ResourceLocation.parse(fragmentShaderSource);
            var resource = manager.getResource(loc);
            fragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthVertexShaderSource = materialJson.get(String.class, "depthVertexSource");
        if (depthVertexShaderSource != null) {
            var loc = ResourceLocation.parse(depthVertexShaderSource);
            var resource = manager.getResource(loc);
            depthVertexShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        String depthFragmentShaderSource = materialJson.get(String.class, "depthFragmentSource");
        if (depthFragmentShaderSource != null) {
            var loc = ResourceLocation.parse(depthFragmentShaderSource);
            var resource = manager.getResource(loc);
            depthFragmentShaderSource = resource.isPresent() ? IOUtils.toString(resource.get().openAsReader()) : null;
        }

        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
        this.depthVertexShaderSource = depthVertexShaderSource;
        this.depthFragmentShaderSource = depthFragmentShaderSource;
    }

}
