package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.ResourceLocation;

public class MaterialMap {

    @Nullable public final Material defaultMaterial;
    public final Map<ResourceLocation, Material> spriteMap = new HashMap<>();

    MaterialMap(JsonObject json) {
        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            ResourceLocation materialLocation = ResourceLocation.parse(defaultMaterialStr);
            this.defaultMaterial = Materials.INSTANCE.get(materialLocation);
        }
        else {
            this.defaultMaterial = null;
        }

        JsonObject defaultMap = JanksonUtils.objectOrEmpty(json, "defaultMap");

        for (JsonObject spriteMapObject : JanksonUtils.listOfObjects(defaultMap, "spriteMap")) {
            String spriteLocationStr = spriteMapObject.get(String.class, "sprite");
            String materialLocationStr = spriteMapObject.get(String.class, "material");
            this.spriteMap.put(
                ResourceLocation.parse(spriteLocationStr),
                Materials.INSTANCE.get(ResourceLocation.parse(materialLocationStr))
            );
        }
    }

}
