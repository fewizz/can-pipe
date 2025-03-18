package fewizz.canpipe.mixininterface;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.VertexConsumer;

import fewizz.canpipe.TangentSetter;
import fewizz.canpipe.material.MaterialMap;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface VertexConsumerExtended extends VertexConsumer {

    float canpipe_getUV(int vertexOffset, int element);

    void canpipe_setAO(float ao);

    void canpipe_recomputeNormal(boolean recompute);

    void canpipe_setSpriteSupplier(Supplier<TextureAtlasSprite> spriteSupplier);

    void canpipe_setSharedMaterialMap(MaterialMap materialMap);

    void canpipe_setSharedGlint(boolean glint);

    void canpipe_setTangent(Consumer<TangentSetter> tangentSupplier);

}
