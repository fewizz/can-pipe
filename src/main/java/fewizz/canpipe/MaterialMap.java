package fewizz.canpipe;

import blue.endless.jankson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public class MaterialMap {

    public final Material defaultMaterial;

    MaterialMap(JsonObject json) {
        ResourceLocation materialLocation = ResourceLocation.parse(json.get(String.class, "defaultMaterial"));
        this.defaultMaterial = Materials.MATERIALS.get(materialLocation);
    }

}
