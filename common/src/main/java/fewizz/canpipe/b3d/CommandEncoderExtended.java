package fewizz.canpipe.b3d;

import java.nio.IntBuffer;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public interface CommandEncoderExtended extends CommandEncoder {

    void canpipe_writeToTexture(
        GpuTexture gpuTexture, IntBuffer intBuffer, Format format, int i, int j, int k, int l, int m, int n,
        VertexFormatElement.Type type  // added
    );

    RenderPass canpipe_createRenderPass(
        Supplier<String> supplier, List<GpuTextureView> colorAttachments, @Nullable GpuTextureView depthAttachment
    );

}
