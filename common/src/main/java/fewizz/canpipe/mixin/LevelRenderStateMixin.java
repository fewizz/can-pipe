package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.mixininterface.LevelRenderStateExtended;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;

@Mixin(LevelRenderState.class)
public class LevelRenderStateMixin implements LevelRenderStateExtended {

    @Unique private ChunkSectionsToRender[] canpipe_chunkSectionsToRender;
    @Unique private List<EntityRenderState>[] canpipe_entityRenderStates;
    @Unique private List<BlockEntityRenderState>[] canpipe_blockEntityRenderStates;
    @Unique private ParticlesRenderState[] canpipe_particlesRenderStates;

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    void onInit(CallbackInfo ci) {
        this.canpipe_chunkSectionsToRender = new ChunkSectionsToRender[4];

        this.canpipe_entityRenderStates =
            Stream.generate(() -> new ArrayList<EntityRenderState>())
            .limit(4).toArray(List[]::new);
        
        this.canpipe_blockEntityRenderStates =
            Stream.generate(() -> new ArrayList<BlockEntityRenderState>())
            .limit(4).toArray(List[]::new);

        this.canpipe_particlesRenderStates = 
            Stream.generate(() -> new ParticlesRenderState())
            .limit(4).toArray(ParticlesRenderState[]::new);
    }

    @Override
    public ChunkSectionsToRender[] canpipe_getChunkSectionsToRender() {
        return this.canpipe_chunkSectionsToRender;
    }

    @Override
    public List<EntityRenderState>[] canpipe_getEntityRenderStates() {
        return this.canpipe_entityRenderStates;
    }

    @Override
    public List<BlockEntityRenderState>[] canpipe_getBlockEntityRenderStates() {
        return this.canpipe_blockEntityRenderStates;
    }

    @Override
    public ParticlesRenderState[] canpipe_getParticlesRenderStates() {
        return this.canpipe_particlesRenderStates;
    }

    @Inject(method = "reset", at = @At("RETURN"))
    void onReset(CallbackInfo ci) {
        for (List<EntityRenderState> canpipe_cascadeEntityRenderStates : this.canpipe_entityRenderStates) {
            canpipe_cascadeEntityRenderStates.clear();
        }
        for (List<BlockEntityRenderState> canpipe_cascadeBlockEntityRenderStates : this.canpipe_blockEntityRenderStates) {
            canpipe_cascadeBlockEntityRenderStates.clear();
        }
    }

}
