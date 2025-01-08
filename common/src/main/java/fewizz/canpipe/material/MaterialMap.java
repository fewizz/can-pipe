package fewizz.canpipe.material;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public class MaterialMap {

    @Nullable public final Material defaultMaterial;

    MaterialMap(JsonObject json) {
        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            ResourceLocation materialLocation = ResourceLocation.parse(defaultMaterialStr);
            this.defaultMaterial = Materials.MATERIALS.get(materialLocation);
        }
        else {
            this.defaultMaterial = null;
        }
    }

}
