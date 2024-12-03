package fewizz.canpipe;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Nullable;
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

        if (materialJson.containsKey("vertexSource")) {
            var loc = ResourceLocation.parse(materialJson.get(String.class, "vertexSource"));
            this.vertexShaderSource = IOUtils.toString(manager.getResourceOrThrow(loc).openAsReader());
        }
        else {
            this.vertexShaderSource = null;
        }

        if (materialJson.containsKey("fragmentSource")) {
            var loc = ResourceLocation.parse(materialJson.get(String.class, "fragmentSource"));
            this.fragmentShaderSource = IOUtils.toString(manager.getResourceOrThrow(loc).openAsReader());
        }
        else {
            this.fragmentShaderSource = null;
        }

    }

}
