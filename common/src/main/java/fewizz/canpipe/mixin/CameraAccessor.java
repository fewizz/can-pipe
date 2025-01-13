package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;

@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("detached")
    public void canpipe_setDetached(boolean value);

    @Accessor("entity")
    public void canpipe_setEntity(Entity entity);

}
