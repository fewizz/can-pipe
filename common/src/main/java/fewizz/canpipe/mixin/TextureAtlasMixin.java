package fewizz.canpipe.mixin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
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

import fewizz.canpipe.b3d.CommandEncoderExtended;
import fewizz.canpipe.mixininterface.TextureAtlasExtended;
import fewizz.canpipe.mixininterface.TextureAtlasSpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin implements TextureAtlasExtended {

    @Shadow @Final private ResourceLocation location;
    @Shadow private Map<ResourceLocation, TextureAtlasSprite> texturesByName;
    @Shadow private int width;
    @Shadow private int height;

    @Unique GpuTexture canpipe_spritesData;
    @Unique GpuTextureView canpipe_spritesDataView;

    @Inject(method = "upload", at = @At("TAIL"))
    void onUploadEnd(CallbackInfo ci) {
        if (this.width > 0x10000 || this.height > 0x10000) {
            throw new RuntimeException("Texture atlas is too big");
        }

        int width = 512;
        int height = Math.ceilDiv(texturesByName.size(), width);

        // 2 - Assuming that max texture size is <= 2^(8*2)
        // 4 - u0, v0, u1, v1
        ByteBuffer buff = MemoryUtil.memAlloc(width*Short.BYTES*4*height).order(ByteOrder.BIG_ENDIAN);
        ShortBuffer shortBuff = buff.asShortBuffer();

        {
            int index = 0;
            for (TextureAtlasSprite s : texturesByName.values()) {
                ((TextureAtlasSpriteExtended) s).setIndex(index);
                shortBuff.put(index*4+0, (short) Mth.floor(s.getU0() * this.width));   // rg
                shortBuff.put(index*4+1, (short) Mth.floor(s.getV0() * this.height));  // ba
                shortBuff.put(index*4+2, (short) Mth.floor(s.getU1() * this.width));   // rg
                shortBuff.put(index*4+3, (short) Mth.floor(s.getV1() * this.height));  // ba
                index += 1;
            }
        }

        try {
            this.canpipe_spritesData = RenderSystem
                .getDevice()
                .createTexture(
                    location.toString()+"-sprites-extents",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8,
                    width*2, height, 1, 1
                );

            var commandEncoder = RenderSystem.getDevice().createCommandEncoder();

            // uses GL_UNSIGNED_BYTE internally, we have this instead:
            ((CommandEncoderExtended) commandEncoder).writeToTexture(
                this.canpipe_spritesData, buff.asIntBuffer(), NativeImage.Format.RGBA, 0, 0, 0, 0, width*2, height
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
