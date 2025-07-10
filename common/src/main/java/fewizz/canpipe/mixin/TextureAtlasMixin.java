package fewizz.canpipe.mixin;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Map;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow @Final private ResourceLocation location;
    @Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;

    @Unique GpuTexture canpipe_spritesData;
    @Unique GpuTextureView canpipe_spritesDataView;

    @Inject(method = "upload", at = @At("TAIL"))
    void onUploadEnd(CallbackInfo ci) {
        int width = 1024;
        int height = Math.ceilDiv(texturesByName.size(), width);
        ByteBuffer byteBuff = MemoryUtil.memAlloc(width*height*4*Float.BYTES);
        FloatBuffer buff = byteBuff.asFloatBuffer();

        {
            int index = 0;
            for (TextureAtlasSprite s : texturesByName.values()) {
                ((TextureAtlasSpriteExtended) s).setIndex(index);
                buff.put(index*4+0, s.getU0());
                buff.put(index*4+1, s.getV0());
                buff.put(index*4+2, s.getU1());
                buff.put(index*4+3, s.getV1());
                index += 1;
            }
        }

        try {
            this.canpipe_spritesData = RenderSystem
                .getDevice()
                .createTexture(
                    location.toString()+"-sprites-extents",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.valueOf("RGBA32F"),
                    width, height, 1, 1
                );

            var commandEncoder = RenderSystem.getDevice().createCommandEncoder();

            // commandEncoder.writeToTexture(
            //     this.canpipe_spritesData, byteBuff.asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height
            // );
            // uses GL_UNSIGNED_BYTE internally, we have this instead:

            ((CommandEncoderExtended) commandEncoder).canpipe_writeToTexture(
                this.canpipe_spritesData, byteBuff.asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, 0, width, height,
                VertexFormatElement.Type.FLOAT
            );

            this.canpipe_spritesDataView = RenderSystem.getDevice().createTextureView(this.canpipe_spritesData);
        } finally {
            MemoryUtil.memFree(buff);
        }
    }

    @Inject(method = "clearTextureData", at = @At("TAIL"))
    public void onClearTextureData(CallbackInfo ci) {
        if (this.canpipe_spritesData != null) {
            this.canpipe_spritesData.close();
        }
    }

    @Override
    public GpuTextureView canpipe_getSpriteData() {
        return this.canpipe_spritesDataView;
    }

    @Override
    public Map<ResourceLocation, TextureAtlasSprite> canpipe_getSprites() {
        return texturesByName;
    }

}
