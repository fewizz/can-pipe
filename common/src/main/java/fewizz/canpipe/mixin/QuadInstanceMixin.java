package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.QuadInstance;

import fewizz.canpipe.mixininterface.QuadInstanceExtended;
import net.minecraft.util.ARGB;

@Mixin(QuadInstance.class)
public class QuadInstanceMixin implements QuadInstanceExtended {

    boolean canpipe_separateShade = false;

    @Shadow private int color0;
	@Shadow private int color1;
	@Shadow private int color2;
	@Shadow private int color3;

    int canpipe_separatedShade0 = -1;
    int canpipe_separatedShade1 = -1;
    int canpipe_separatedShade2 = -1;
    int canpipe_separatedShade3 = -1;

    @Inject(method = "scaleColor", at = @At("HEAD"), cancellable = true)
    void onScaleColor(final float scale, CallbackInfo ci) {
        if (this.canpipe_separateShade) {
            ci.cancel();
            return; //  Don't shade
        }
    }

    @Inject(method = "setColor(I)V", at = @At("RETURN"))
    void onSetColor(CallbackInfo ci) {
        if (this.canpipe_separateShade) {
            this.canpipe_separatedShade0 = this.color0;
            this.canpipe_separatedShade1 = this.color1;
            this.canpipe_separatedShade2 = this.color2;
            this.canpipe_separatedShade3 = this.color3;

            this.color0 = 0xFFFFFFFF;
            this.color1 = 0xFFFFFFFF;
            this.color2 = 0xFFFFFFFF;
            this.color3 = 0xFFFFFFFF;
        }
        else {
            this.canpipe_separatedShade0 = 0xFFFFFFFF;
            this.canpipe_separatedShade1 = 0xFFFFFFFF;
            this.canpipe_separatedShade2 = 0xFFFFFFFF;
            this.canpipe_separatedShade3 = 0xFFFFFFFF;
        }
    }

    @Inject(method = "setColor(II)V", at = @At("RETURN"))
    void onSetColor(final int vertex, final int color, CallbackInfo ci) {
        if (this.canpipe_separateShade) {
            switch (vertex) {
                case 0: this.canpipe_separatedShade0 = this.color0; this.color0 = 0xFFFFFFFF; break;
                case 1: this.canpipe_separatedShade1 = this.color1; this.color1 = 0xFFFFFFFF; break;
                case 2: this.canpipe_separatedShade2 = this.color2; this.color2 = 0xFFFFFFFF; break;
                case 3: this.canpipe_separatedShade3 = this.color3; this.color3 = 0xFFFFFFFF; break;
                default: throw new IndexOutOfBoundsException();
            }
        }
    }

    @Override
    public void canpipe_separateScale(boolean value) {
        this.canpipe_separateShade = value;
    }

    @Override
    public float canpipe_getSeparatedShade(int index) {
        return switch (index) {
            case 0 -> ARGB.redFloat(this.canpipe_separatedShade0);
            case 1 -> ARGB.redFloat(this.canpipe_separatedShade1);
            case 2 -> ARGB.redFloat(this.canpipe_separatedShade2);
            case 3 -> ARGB.redFloat(this.canpipe_separatedShade3);
            default -> throw new IndexOutOfBoundsException();
        };
    }

}
