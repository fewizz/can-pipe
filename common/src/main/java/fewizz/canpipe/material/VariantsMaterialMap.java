package fewizz.canpipe.material;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.JanksonUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.Property.Value;

public record VariantsMaterialMap<S extends StateHolder<?, ?>>(
    @Nullable MaterialMap defaultMap,
    Map<S, MaterialMap> variants
) {

    static <S extends StateHolder<?, ?>> VariantsMaterialMap<S> load(JsonObject json, List<S> states) {
        MaterialMap defaultMap = null;

        String defaultMaterialStr = json.get(String.class, "defaultMaterial");
        if (defaultMaterialStr != null) {
            Identifier materialLocation = Identifier.parse(defaultMaterialStr);
            Material defaultMaterial = Materials.get(materialLocation);
            defaultMap = new MaterialMap(defaultMaterial, Map.of());
        }

        String defaultMapStr = json.get(String.class, "defaultMap");
        if (defaultMapStr != null) {
            MaterialMap possibleDefaultMap = MaterialMap.load(json);
            if (possibleDefaultMap != null) {
                defaultMap = possibleDefaultMap;
            }
        }

        Map<S, MaterialMap> variants = new HashMap<>();
        JsonObject variantsJson = JanksonUtils.objectOrEmpty(json, "variants");
        for (S state : states) {
            String stateStr = statePropertiesToString(state.getValues());

            JsonObject stateMaterialMapJson = variantsJson.getObject(stateStr);
            MaterialMap stateMap = null;

            if (stateMaterialMapJson != null) {
                stateMap = MaterialMap.load(stateMaterialMapJson);
            }

            MaterialMap map = stateMap != null ? stateMap : defaultMap;
            if (map != null) {
                variants.put(state, map);
            }
        }

        return new VariantsMaterialMap<S>(defaultMap, variants);
    }

    private static String statePropertiesToString(Stream<Value<?>> values) {
        StringBuilder stringBuilder = new StringBuilder();

        values.forEach(v -> {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(',');
            }

            Property<?> property = v.property();
            stringBuilder.append(property.getName());
            stringBuilder.append('=');
            stringBuilder.append(v.valueName());
        });

        return stringBuilder.toString();
    }

    public MaterialMap forState(BlockState state) {
        MaterialMap result = this.variants.get(state);
        return result == null ? this.defaultMap : result;
    }

    Set<Material> getUsedMaterials() {
        Set<Material> result = new HashSet<>();
        if (this.defaultMap != null) {
            result.addAll(this.defaultMap.getUsedMaterials());
        }
        for (var variant : this.variants.values()) {
            result.addAll(variant.getUsedMaterials());
        }
        return result;
    }

}
