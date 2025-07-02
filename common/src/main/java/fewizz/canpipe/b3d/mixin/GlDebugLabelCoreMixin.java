package fewizz.canpipe.b3d.mixin;

import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.KHRDebug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fewizz.canpipe.b3d.GlDebugLabelExtended;
import net.minecraft.util.StringUtil;

@Mixin(targets = {"com.mojang.blaze3d.opengl.GlDebugLabel$Core"})
public class GlDebugLabelCoreMixin implements GlDebugLabelExtended {

    @Shadow @Final private int maxLabelLength;

    @Override
    public void canpipe_applyLabelFramebuffer(int id, String label) {
        KHRDebug.glObjectLabel(
            GL33C.GL_FRAMEBUFFER,
            id,
            StringUtil.truncateStringIfNecessary(label, this.maxLabelLength, true)
        );
    }

}
