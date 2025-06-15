package fewizz.canpipe.mixin.m03_core;

import org.lwjgl.opengl.EXTDebugLabel;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;

import fewizz.canpipe.mixininterface.GlDebugLabelExtended;
import net.minecraft.util.StringUtil;

@Mixin(targets = {"com.mojang.blaze3d.opengl.GlDebugLabel$Ext"})
public class GlDebugLabelExtMixin implements GlDebugLabelExtended {

    @Override
    public void canpipe_applyLabelFramebuffer(int id, String label) {
        EXTDebugLabel.glLabelObjectEXT(
            GL33C.GL_FRAMEBUFFER,
            id,
            StringUtil.truncateStringIfNecessary(label, 256, true)
        );
    }

}
