package fewizz.canpipe.mixininterface;

import java.util.List;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;

public interface LevelRenderStateExtended {

    List<EntityRenderState>[] canpipe_getEntityRenderStates();

    ParticlesRenderState[] canpipe_getParticlesRenderStates();

}
