package fewizz.canpipe.helpers;

import java.util.LinkedHashMap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class BBPerRenderTypeMultiBufferSource extends MultiBufferSource.BufferSource implements AutoCloseable {

    public BBPerRenderTypeMultiBufferSource() {
        super(null, new LinkedHashMap<>());
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return this.startedBuilders.computeIfAbsent(
            renderType,
            rt -> new BufferBuilder(
                this.fixedBuffers.computeIfAbsent(rt, _rt -> new ByteBufferBuilder(0xF_FF_FF)),
                rt.mode(),
                rt.format()
            )
        );
    }

    @Override
    public void close() throws Exception {
        for (var bbb : this.fixedBuffers.values()) {
            bbb.close();
        }
    }

}
