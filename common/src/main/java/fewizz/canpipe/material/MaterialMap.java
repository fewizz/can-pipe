package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;

public class MaterialMap {

    @Nullable public final Material defaultMaterial;
    public final Map<Identifier, Material> spriteMap = new HashMap<>();

    MaterialMap(JsonObject json) {
        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            Identifier materialLocation = Identifier.parse(defaultMaterialStr);
            this.defaultMaterial = Materials.get(materialLocation);
        }
        else {
            this.defaultMaterial = null;
        }

        JsonObject defaultMap = JanksonUtils.objectOrEmpty(json, "defaultMap");

        for (JsonObject spriteMapObject : JanksonUtils.listOfObjects(defaultMap, "spriteMap")) {
            String spriteLocationStr = spriteMapObject.get(String.class, "sprite");
            String materialLocationStr = spriteMapObject.get(String.class, "material");
            this.spriteMap.put(
                Identifier.parse(spriteLocationStr),
                Materials.get(Identifier.parse(materialLocationStr))
            );
        }
    }

    boolean usesMaterial(Material material) {
        return this.defaultMaterial == material || this.spriteMap.values().contains(material);
    }

}
