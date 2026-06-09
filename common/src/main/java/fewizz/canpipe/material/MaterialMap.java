package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public record MaterialMap(@Nullable Material defaultMaterial, Map<Identifier, Material> spriteMap) {

    static MaterialMap load(JsonObject json) {
        Material defaultMaterial = null;
        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            Identifier materialLocation = Identifier.parse(defaultMaterialStr);
            defaultMaterial = Materials.get(materialLocation);
        }

        Map<Identifier, Material> spriteMap = new HashMap<>();
        for (JsonObject spriteMapObject : JanksonUtils.listOfObjects(json, "spriteMap")) {
            Identifier spriteId = Identifier.parse(JanksonUtils.stringOrThrow(spriteMapObject, "sprite"));
            Identifier materialId = Identifier.parse(JanksonUtils.stringOrThrow(spriteMapObject, "material"));
            spriteMap.put(spriteId, Materials.get(materialId));
        }
        return new MaterialMap(defaultMaterial, spriteMap);
    }

    static @Nullable MaterialMap loadParticle(JsonObject json) {
        String materialLocationString = json.get(String.class, "material");
        if (materialLocationString == null) {
            return null;
        }
        Identifier materialLocation = Identifier.parse(materialLocationString);
        Material material = Materials.get(materialLocation);
        if  (material == null) {
            return null;
        }
        return new MaterialMap(material, Map.of());
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

    public Material getMaterial(@Nullable TextureAtlasSprite atlasSprite) {
        Material material = null;

        if (this.spriteMap != null && atlasSprite != null) {
            Minecraft mc = Minecraft.getInstance();
            TextureAtlas atlas = (TextureAtlas) mc.getTextureManager().getTexture(atlasSprite.atlasLocation());

            for (var kv : this.spriteMap.entrySet()) {
                if (atlas.getSprite(kv.getKey()) == atlasSprite) {
                    material = kv.getValue();
                    break;
                }
            }
        }

        if (material == null) {
            material = this.defaultMaterial;
        }

        return material;
    }

}
