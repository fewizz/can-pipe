package fewizz.canpipe;

import blue.endless.jankson.JsonArray;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class Material {

    @Nullable final ResourceLocation vertexShaderSourceLocation;
    @Nullable final ResourceLocation fragmentShaderSourceLocation;

    Material(
        ResourceManager manager,
        ResourceLocation location,
        JsonObject materialJson
    ) {
        var layers = (JsonArray) materialJson.get("layers");
        if (layers.size() > 0) {
            JanksonUtils.mergeJsonObjectB2A(materialJson, (JsonObject) layers.get(0));
        }

        if (materialJson.containsKey("vertexSource")) {
            this.vertexShaderSourceLocation = ResourceLocation.parse(materialJson.get(String.class, "vertexSource"));
        }
        else {
            this.vertexShaderSourceLocation = null;
        }

        if (materialJson.containsKey("fragmentSource")) {
            this.fragmentShaderSourceLocation = ResourceLocation.parse(materialJson.get(String.class, "fragmentSource"));
        }
        else {
            this.fragmentShaderSourceLocation = null;
        }

    }

}
