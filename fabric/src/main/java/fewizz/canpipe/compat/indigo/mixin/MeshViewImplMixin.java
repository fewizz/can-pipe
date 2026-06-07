package fewizz.canpipe.compat.indigo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MeshViewImpl;

@Mixin(MeshViewImpl.class)
public class MeshViewImplMixin {

    @Unique int[] canpipe_data;

    

}
