package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.CanPipe;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {

    public VideoSettingsScreenMixin() {
        super(null, null, null);
    }

    @Inject(
        method = "addOptions",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V",
            ordinal = 1,
            shift = Shift.AFTER
        )
    )
    private void addPipelineOptions(CallbackInfo ci) {
        var butt = CanPipe.PIPELINE_OPTION.createButton(this.options);
        butt.setWidth(butt.getWidth() + 130);
        list.addSmall(
            butt,
            new Button(0, 0, 20, 20, Component.literal("*"), (b) -> {}, null) {

                public void setX(int x) {
                    super.setX(x + 130);
                };

            }
        );
    }

}
