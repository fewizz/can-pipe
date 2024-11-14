package fewizz.canpipe.mixininterface;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface VertexConsumerExtended extends VertexConsumer {

    void setAO(float ao);
    void setMaterial(int material);
    void setTangent(Vector3f tangent);

}
