package fewizz.canpipe.material;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;

public record EntityMaterialMap(
    @Nullable Material defaultMaterial,
    List<MaterialPredicates> materialsByPredicates
) {

    public record MaterialPedicateContext(
        Identifier textureID,
        String renderLayerName
    ) {}

    public interface MaterialPredicate {
        boolean test(MaterialPedicateContext ctx);
    }

    public record MaterialPredicates(
        List<MaterialPredicate> predicates,
        Material material
    ) {}

    static EntityMaterialMap load(Identifier id, List<JsonObject> jsons) {
        Material defaultMaterial = null;

        // from top to bottom https://github.com/vram-guild/frex/blob/dbfb312dd1ed25b4d3cd1c75c1eb1c77c4087ead/common/src/main/java/io/vram/frex/impl/material/map/EntityMaterialMapDeserializer.java#L68
        for (var json : jsons.reversed()) {
            String defaultMaterialStr = json.get(String.class, "defaultMaterial");
            if (defaultMaterialStr == null) { continue; }

            defaultMaterial = Materials.get(Identifier.parse(defaultMaterialStr));

            if (defaultMaterial != null) {
                break;
            }
        }

        List<MaterialPredicates> materialsByPredicate = new ArrayList<>();

        for (var json : jsons) {
            for (JsonObject entry : JanksonUtils.listOfObjects(json, "map")) {
                MaterialPredicates materialPredicates = tryLoadMaterialPredicates(id, entry);
                if (materialPredicates != null) {
                    materialsByPredicate.add(materialPredicates);
                }
            }
        }

        return new EntityMaterialMap(defaultMaterial, materialsByPredicate);
    }

    static private MaterialPredicates tryLoadMaterialPredicates(Identifier id, JsonObject entry) {
        JsonObject predicateJson = JanksonUtils.objectOrThrow(entry, "predicate");
        JsonObject materialPredicateJson = predicateJson.getObject("materialPredicate");
        JsonObject entityPredicateJson = predicateJson.getObject("entityPredicate");

        if (entityPredicateJson != null) {
            CanPipe.LOGGER.warn("Entity material map \""+id+"\": entity predicates aren't supported");
            return null;
        }

        if (materialPredicateJson == null) {
            CanPipe.LOGGER.warn("Entity material map \""+id+"\": no material predicate");
            return null;
        }

        List<MaterialPredicate> predicates = new ArrayList<>();

        for (var kv : materialPredicateJson.entrySet()) {
            String predicateName = kv.getKey();
            JsonPrimitive predicateValue = (JsonPrimitive) kv.getValue();
            switch (predicateName) {
                case "texture" -> {
                    Identifier textureID = Identifier.parse(predicateValue.asString());
                    predicates.add(ctx -> ctx.textureID.equals(textureID));
                }
                case "renderLayerName" -> {
                    String expectedRenderLayerName = predicateValue.asString();
                    predicates.add(ctx -> expectedRenderLayerName.equals(ctx.renderLayerName));
                }
                default -> {
                    CanPipe.LOGGER.warn("Entity material map \""+id+"\": unsupported predicate \""+predicateName+"\"");
                    return null;
                }
            }
        }

        Identifier materialId = Identifier.parse(JanksonUtils.stringOrThrow(entry, "material"));
        Material material = Materials.get(materialId);

        return new MaterialPredicates(predicates, material);
    }

    Set<Material> getUsedMaterials() {
        Set<Material> result = new HashSet<>();
        if (this.defaultMaterial != null) {
            result.add(this.defaultMaterial);
        }
        for (var materialByPredicates : this.materialsByPredicates) {
            result.add(materialByPredicates.material);
        }
        return result;
    }

}
