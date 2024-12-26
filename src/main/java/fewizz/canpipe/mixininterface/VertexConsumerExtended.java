package fewizz.canpipe.mixininterface;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface VertexConsumerExtended extends VertexConsumer {

    static float[] NO_AO = new float[] {1.0F, 1.0F, 1.0F, 1.0F};

    void setAO(float ao);
    void recomputeNormal(boolean recompute);

    void setSharedSpriteIndex(int spriteIndex);
    void inheritSpriteIndex();

    void setSharedMaterialIndex(int materialIndex);
    void inheritMaterialIndex();
    default void resetSharedMaterialIndex() {
        this.setSharedMaterialIndex(-1);
    }

    void setSharedTangent(float x, float y, float z);
    void inheritTangent();
    default void resetSharedTangent() {
        this.setSharedTangent(1.0F, 0.0F, 0.0F);
    }

}
