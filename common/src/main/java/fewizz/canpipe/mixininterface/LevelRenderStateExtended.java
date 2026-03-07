package fewizz.canpipe.mixininterface;

import java.util.List;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;

public interface LevelRenderStateExtended {

    ChunkSectionsToRender[] canpipe_getChunkSectionsToRender();
    List<EntityRenderState>[] canpipe_getEntityRenderStates();
    List<BlockEntityRenderState>[] canpipe_getBlockEntityRenderStates();
    ParticlesRenderState[] canpipe_getParticlesRenderStates();

}
