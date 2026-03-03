package fewizz.canpipe.mixininterface;

import java.util.List;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public interface LevelRenderStateExtended {

    List<EntityRenderState>[] canpipe_getEntityRenderStates();

}
