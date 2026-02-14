package fewizz.canpipe.mixininterface;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface VertexConsumerExtended extends VertexConsumer {

    VertexFormat canpipe_getVertexFormat();
    float canpipe_getUV(int vertexOffset, int element);
    void canpipe_setPendingAO(float ao);  // Will be applied on next `addVertex`
    void canpipe_recomputeNormal(boolean recompute);
    void canpipe_setSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier);
    void canpipe_setSharedMaterialMap(MaterialMap materialMap);
    void canpipe_setSharedGlint(boolean glint);

}
