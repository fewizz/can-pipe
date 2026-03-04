package fewizz.canpipe.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

import fewizz.canpipe.mixininterface.LightmapExtended;
import net.minecraft.client.renderer.Lightmap;

@Mixin(Lightmap.class)
public class LightmapMixin implements LightmapExtended {

    @Shadow @Final private GpuTexture texture;

    @Unique private Vector4f canpipe_emissiveColor = new Vector4f(1.0F);
    @Unique private GpuBuffer canpipe_emissiveColorReadGpuBuffer;


    @Inject(method = "<init>", at = @At("TAIL"))
    void onInit(CallbackInfo ci) {
        canpipe_emissiveColorReadGpuBuffer = RenderSystem.getDevice().createBuffer(
            () -> "can-pipe light texture read buffer",
            GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
            this.texture.getWidth(0)*this.texture.getHeight(0)*this.texture.getFormat().pixelSize()
        );
    }

    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture("+
                "Ljava/lang/String;"+
                "I"+
                "Lcom/mojang/blaze3d/textures/TextureFormat;"+
                "IIII"+
            ")Lcom/mojang/blaze3d/textures/GpuTexture;"
        ),
        index = 1
    )
    int onTextureInit(int usage) {
        return usage | GpuTexture.USAGE_COPY_SRC;
    }

    @Override
    public Vector4f canpipe_getEmissiveColor() {
        return this.canpipe_emissiveColor;
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            // right after draw
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V",
            shift = Shift.AFTER
        )
    )
    void fetchEmissiveColor(CallbackInfo ci) {
        // For some reason, validation in copyTextureToBuffer don't take x, y into account,
        // so i have to copy whole texture
        RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(
            this.texture,
            this.canpipe_emissiveColorReadGpuBuffer,
            0,  // offset
            () -> {  // Past one frame, but anyway...
                try (var readView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.canpipe_emissiveColorReadGpuBuffer, true, false)) {
                    int pos = this.texture.getFormat().pixelSize()*this.texture.getHeight(0)*(this.texture.getWidth(0) - 1);
                    this.canpipe_emissiveColor.x = Byte.toUnsignedInt(readView.data().get(pos+0)) / 255.0F;
                    this.canpipe_emissiveColor.y = Byte.toUnsignedInt(readView.data().get(pos+1)) / 255.0F;
                    this.canpipe_emissiveColor.z = Byte.toUnsignedInt(readView.data().get(pos+2)) / 255.0F;
                    this.canpipe_emissiveColor.w = Byte.toUnsignedInt(readView.data().get(pos+3)) / 255.0F;
                }
            },
            0  // level
        );
    }

}
