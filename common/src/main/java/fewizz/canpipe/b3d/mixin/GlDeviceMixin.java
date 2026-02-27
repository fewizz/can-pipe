package fewizz.canpipe.b3d.mixin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;

import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlShaderModule;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import fewizz.canpipe.b3d.GpuDeviceBackendExtended;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements GpuDeviceBackendExtended {

    @Shadow abstract protected GlShaderModule getOrCompileShader(Identifier shader, ShaderType type, ShaderDefines defines, ShaderSource shaderSource);

    @Unique private TriConsumer<String, Identifier, String> canpipe_onCompilationError = null;
    @Unique private String canpipe_compilationLog = null;

    @Unique private int canpipe_pendingTextureViewBaseLayer = -1;
    @Unique private int canpipe_pendingTextureViewLayerCount = -1;

    @Unique private AddressMode canpipe_addressModeW = null;
    @Unique private DepthTestFunction canpipe_compareOp = null;
    @Unique private Boolean canpipe_linearMipmap = null;

    /** Color textures + depth texture at the end (nullable) **/
    @Unique private Object2IntMap<List<GlTextureView>> canpipe_framebufferCache = new Object2IntOpenHashMap<>();

    @Override
    public GpuSampler canpipe_createSampler(
        AddressMode u, AddressMode v,
        FilterMode min, FilterMode mag,
        int maxAnisotropy, OptionalDouble maxLod,
        AddressMode w, @Nullable DepthTestFunction compareOp, boolean linearMipmap
    ) {
        try {
            this.canpipe_addressModeW = w;
            this.canpipe_compareOp = compareOp;
            this.canpipe_linearMipmap = linearMipmap;
            return this.createSampler(u, v, min, mag, maxAnisotropy, maxLod);
        } finally {
            this.canpipe_addressModeW = null;
            this.canpipe_compareOp = null;
            this.canpipe_linearMipmap = null;
        }
    }

    @Override
    public void canpipe_precompilePipelineShaderModules(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        TriConsumer<String, Identifier, String> onCompilationError
    ) {
        try {
            this.canpipe_onCompilationError = onCompilationError;
            this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
            this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        } finally {
            this.canpipe_onCompilationError = null;
            this.canpipe_compilationLog = null;
        }
    }

    @Override
    public GpuTextureView canpipe_createTextureView(
        GpuTexture gpuTexture, int baseMip, int levelCount,
        int baseLayer, int layerCount // added
    ) {
        try {
            this.canpipe_pendingTextureViewBaseLayer = baseLayer;
            this.canpipe_pendingTextureViewLayerCount = layerCount;
            return this.createTextureView(gpuTexture, baseMip, levelCount);
        }
        finally {
            this.canpipe_pendingTextureViewBaseLayer = -1;
            this.canpipe_pendingTextureViewLayerCount = -1;
        }
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(value = "CONSTANT", args = "intValue=32768")
    )
    int extendMaxLogLength(int original, @Local(index = 0) int id) {
        int logLength = GlStateManager.glGetShaderi(id, GL33C.GL_INFO_LOG_LENGTH);
        return logLength;
    }

    @ModifyExpressionValue(
        method = "compileShader",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;glGetShaderInfoLog(II)Ljava/lang/String;"
        )
    )
    String catchLog(String log) {
        this.canpipe_compilationLog = log;
        return log;
    }

    @ModifyReturnValue(method = "compileShader", at = @At("RETURN"))
    GlShaderModule onCompilationError(
        GlShaderModule module,
        @Local(ordinal = 0) String source,
        @Local(ordinal = 0) GlDevice.ShaderCompilationKey key
    ) {
        if (module == GlShaderModule.INVALID_SHADER && this.canpipe_onCompilationError != null) {
            this.canpipe_onCompilationError.accept(this.canpipe_compilationLog, key.id(), source);
        }
        return module;
    }

    @Inject(
        method = "createTexture("+
            "Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII"+
        ")Lcom/mojang/blaze3d/textures/GpuTexture;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_bindTexture(I)V"
        )
    )
    void beforeBindingTexture2D(
        CallbackInfoReturnable<Void> ci,
        @Local(argsOnly = true, ordinal = 3) int layers,
        @Local(ordinal = 5) int id
    ) {
        if (layers > 1) {
            GlStateManagerAccessor.canpipe_setTextureTarget(id, GL33C.GL_TEXTURE_2D_ARRAY);
        }
    }

    @ModifyExpressionValue(
        method = "createTexture("+
            "Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII"+
        ")Lcom/mojang/blaze3d/textures/GpuTexture;",
        at = @At(value = "CONSTANT", args = "intValue=3553")  // GL_TEXTURE_2D
    )
    int changeTarget(int target, @Local(argsOnly = true, ordinal = 3) int layers) {
        if (layers > 1) {
            target = GL33C.GL_TEXTURE_2D_ARRAY;
        }
        return target;
    }

    @WrapWithCondition(
        method = "createTexture("+
            "Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII"+
        ")Lcom/mojang/blaze3d/textures/GpuTexture;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texImage2D(IIIIIIIILjava/nio/ByteBuffer;)V",
            ordinal = 1
        )
    )
    boolean texImage3DIfLayerGreaterThanOne(
        int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels,
        @Local(argsOnly = true, ordinal = 3) int layers
    ) {
        if (layers > 1) {
            GL33C.glTexImage3D(GL33C.GL_TEXTURE_2D_ARRAY, level, internalFormat, width, height, layers, 0, format, type, pixels);
            return false;
        }
        return true;
    }

}
