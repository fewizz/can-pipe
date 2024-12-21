package fewizz.canpipe.material;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Nullable;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class Material {

    public final ResourceLocation location;
    @Nullable public final String vertexShaderSource;
    @Nullable public final String fragmentShaderSource;

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

        String vertexShaderSource = null;
        String fragmentShaderSource = null;

        if (materialJson.containsKey("vertexSource")) {
            var loc = ResourceLocation.parse(materialJson.get(String.class, "vertexSource"));
            var resource = manager.getResource(loc);
            if (resource.isPresent()) {
                vertexShaderSource = IOUtils.toString(resource.get().openAsReader());
            }
        }

        if (materialJson.containsKey("fragmentSource")) {
            var loc = ResourceLocation.parse(materialJson.get(String.class, "fragmentSource"));
            var resource = manager.getResource(loc);
            if (resource.isPresent()) {
                fragmentShaderSource = IOUtils.toString(resource.get().openAsReader());
            }
        }

        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;

    }

}
