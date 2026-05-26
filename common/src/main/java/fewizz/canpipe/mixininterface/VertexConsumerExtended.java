package fewizz.canpipe.mixininterface;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.material.Material;
import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public interface VertexConsumerExtended extends VertexConsumer {

    VertexFormat canpipe_getVertexFormat();

    /**
     * Will be applied on next <code>addVertex()</code> only
     * (i.e., it should be called before <code>addVertex()</code>)
     * If not provided, <code>1.0F</code> will be used.
     */
    void canpipe_setPendingAO(float ao);

    /**
     * Persistent state. If enabled, <code>addNormal()</code> will be ignored,
     * and normal will be recalculated in <code>endLastVertex()</code>
     */
    void canpipe_forceNormalRecomputation(boolean recompute);

    void canpipe_setScopedSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier);

    void canpipe_setScopedMaterialMap(MaterialMap materialMap);
    void canpipe_setScopedMaterial(Material material);  // If material is set, material map will be ignored

    void canpipe_setScopedTextureIdentifier(Identifier textureIdentifier);

    void canpipe_setScopedGlint(boolean glint);
    void canpipe_setScopedEntityGlint(boolean glint);

    float canpipe_getU(int vertexOffset);
    float canpipe_getV(int vertexOffset);

}
