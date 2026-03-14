package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;

public record MaterialMap(@Nullable Material defaultMaterial, Map<Identifier, Material> spriteMap) {

    static MaterialMap load(JsonObject json) {
        Material defaultMaterial = null;
        Map<Identifier, Material> spriteMap = new HashMap<>();

        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            Identifier materialLocation = Identifier.parse(defaultMaterialStr);
            defaultMaterial = Materials.get(materialLocation);
        }
        else {
            defaultMaterial = null;
        }

        JsonObject defaultMap = JanksonUtils.objectOrEmpty(json, "defaultMap");

        for (JsonObject spriteMapObject : JanksonUtils.listOfObjects(defaultMap, "spriteMap")) {
            String spriteLocationStr = spriteMapObject.get(String.class, "sprite");
            String materialLocationStr = spriteMapObject.get(String.class, "material");
            spriteMap.put(
                Identifier.parse(spriteLocationStr),
                Materials.get(Identifier.parse(materialLocationStr))
            );
        }
        return new MaterialMap(defaultMaterial, spriteMap);
    }

    static MaterialMap loadEntity(JsonObject json) {
        Map<Identifier, Material> spriteMap = new HashMap<>();

        for (JsonObject entry : JanksonUtils.listOfObjects(json, "map")) {
            JsonObject predicate = entry.getObject("predicate");
            JsonObject materialPredicate = predicate.getObject("materialPredicate");
            String textureIdStr = materialPredicate.get(String.class, "texture");
            if (textureIdStr == null) {
                continue;
            }
            Identifier textureId = Identifier.parse(textureIdStr);
            textureId = CanPipe.upgradeResourcePath(textureId);

            Identifier materialId = Identifier.parse(entry.get(String.class, "material"));
            Material material = Materials.get(materialId);

            spriteMap.put(textureId, material);
        }

        return new MaterialMap(null, spriteMap);
    }

    boolean usesMaterial(Material material) {
        return this.defaultMaterial == material || this.spriteMap.values().contains(material);
    }

    Set<Material> getUsedMaterials() {
        Set<Material> result = new HashSet<>();
        if (this.defaultMaterial != null) {
            result.add(this.defaultMaterial);
        }
        for (var material : this.spriteMap.values()) {
            if (material != null) {
                result.add(material);
            }
        }
        return result;
    }

}
