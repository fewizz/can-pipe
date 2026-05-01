package fewizz.canpipe.helpers;

import java.util.Collections;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class NullBufferSource extends MultiBufferSource.BufferSource {

    public NullBufferSource() {
        super(null, Collections.emptyNavigableMap());
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new VertexConsumer() {

            @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
            @Override public VertexConsumer setColor(int color) { return this; }
            @Override public VertexConsumer setUv(float u, float v) { return this; }
            @Override public VertexConsumer setUv1(int u, int v) { return this; }
            @Override public VertexConsumer setUv2(int u, int v) { return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
            @Override public VertexConsumer setLineWidth(float width) { return this; }

        };
    }

}
