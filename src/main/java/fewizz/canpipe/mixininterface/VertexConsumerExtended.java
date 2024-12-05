package fewizz.canpipe.mixininterface;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface VertexConsumerExtended extends VertexConsumer {

    void setAO(float ao);

    void setSharedSpriteIndex(int spriteIndex);
    void inheritSpriteIndex();

    void setSharedMaterialIndex(int materialIndex);
    void inheritMaterialIndex();

    void setSharedTangent(float x, float y, float z);
    void inheritTangent();
    default void resetSharedTangent() {
        this.setSharedTangent(1.0F, 0.0F, 0.0F);
    }

}
