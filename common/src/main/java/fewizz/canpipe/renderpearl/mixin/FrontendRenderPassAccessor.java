package fewizz.canpipe.renderpearl.mixin;

import java.util.List;
import java.util.Optional;

import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.frontend.FrontendRenderPass;

@Mixin(FrontendRenderPass.class)
public interface FrontendRenderPassAccessor {

    @Accessor("colorAttachments")
    List<RenderPassDescriptor.@Nullable Attachment<Optional<Vector4fc>>> canpipe_getColorAttachments();

}
