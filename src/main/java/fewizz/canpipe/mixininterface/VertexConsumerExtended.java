package fewizz.canpipe.mixininterface;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface VertexConsumerExtended extends VertexConsumer {

    void setAO(float ao);
    void setSpriteIndex(int material);
    void setTangent(int tangentPacked);

}
