package fewizz.canpipe.mixin;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.material.Material;
import fewizz.canpipe.mixininterface.QuadParticleRenderStateStorageExtended;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;

@Mixin(QuadParticleRenderState.Storage.class)
public class QuadParticleRenderStateStorageMixin implements QuadParticleRenderStateStorageExtended {

    @Shadow private int capacity;
    @Shadow private int currentParticleIndex;

    @Unique private short[] canpipe_materialValues;

    @Inject(method = "<init>", at = @At("TAIL"))
    void onInit(CallbackInfo ci) {
        this.canpipe_materialValues = new short[this.capacity];
    }

    @Inject(method = "grow", at = @At("TAIL"))
    void onGrow(CallbackInfo ci) {
        this.canpipe_materialValues = Arrays.copyOf(this.canpipe_materialValues, this.capacity);
    }

    @Override
    public void canpipe_alsoAddMaterial(@Nullable Material material) {
        short index = material != null ? material.index() : -1;
        this.canpipe_materialValues[this.currentParticleIndex-1] = index;
    }

    @Override
    public short[] canpipe_materialsValues() {
        return this.canpipe_materialValues;
    }

}
