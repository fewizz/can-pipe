package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.PipelineOptionsScreen;
import fewizz.canpipe.mixininterface.VideoSettingsScreenExtended;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.PipelineRaw;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen implements VideoSettingsScreenExtended {

    VideoSettingsScreenMixin() { super(null, null, null); }

    MutableObject<Button> settingsButtonRef = new MutableObject<>();
    MutableObject<CycleButton<Optional<PipelineRaw>>> pipelineButtonRef = new MutableObject<>();

    @Override
    public void canpipe_onPipelineLoaded() {
        boolean showSettings =
            Pipelines.getCurrentRaw() != null &&
            Pipelines.getLoadingError() == null;

        pipelineButtonRef.getValue().setWidth(Button.DEFAULT_WIDTH + 10 + Button.DEFAULT_WIDTH - (showSettings ? 30 : 0));
        pipelineButtonRef.getValue().setValue(Optional.ofNullable(Pipelines.getCurrentRaw()));
        settingsButtonRef.getValue().visible = showSettings;
    }

    @SuppressWarnings("unchecked")
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
        var settingsButton = new Button(
            0, 0,
            20, 20,
            Component.literal("S"),
            (button) -> {
                Pipeline current = Pipelines.getCurrent();
                if (current == null) {
                    return;
                }
                this.minecraft.setScreen(new PipelineOptionsScreen(
                    (Screen)(Object) this,
                    Pipelines.RAW_PIPELINES.get(current.location),
                    current.appliedOptions
                ));
            },
            null
        ) {
            @Override
            public void setX(int x) {
                super.setX(x + 130);
            };
        };

        var pipelineButton = (CycleButton<Optional<PipelineRaw>>) new OptionInstance<Optional<PipelineRaw>>(
            "Pipeline",
            (Optional<PipelineRaw> p) -> {  // widget tooltip
                var e = Pipelines.getLoadingError();
                if (e == null) return null;
                return Tooltip.create(Component.literal(e.getMessage()));
            },
            (Component c, Optional<PipelineRaw> p) -> {  // widget's new message, before on value changed
                // no pipeline is loaded
                if (p.isEmpty()) return Component.literal("No");

                var component = Component.translatable(p.get().nameKey);
                // there was an unsuccessful try to load pipeline,
                // make label reddish
                if (Pipelines.getCurrent() == null) {
                    component.withColor(0xFF7777);
                }
                return component;
            },
            new OptionInstance.LazyEnum<Optional<PipelineRaw>>(
                () -> {  // possible values
                    var values = new ArrayList<Optional<PipelineRaw>>();
                    values.add(Optional.empty());
                    values.addAll(
                        Pipelines.RAW_PIPELINES.values().stream()
                            .map(p -> Optional.of(p))
                            .collect(Collectors.toList())
                    );
                    return values;
                },
                (Optional<PipelineRaw> val) -> Optional.of(val),
                null
            ),
            Optional.ofNullable(Pipelines.getCurrentRaw()),  // current widget value
            (Optional<PipelineRaw> p) -> {  // on widget value changed
                if (Pipelines.getCurrentRaw() == p.orElse(null)) return;

                Pipelines.loadAndSetPipeline(p.orElse(null), Map.of());
                // update value to update label
                // first check is to avoid recursive pipeline loading
                canpipe_onPipelineLoaded();
            }
        ).createButton(this.options);

        settingsButtonRef.setValue(settingsButton);
        pipelineButtonRef.setValue(pipelineButton);

        canpipe_onPipelineLoaded();

        list.addSmall(pipelineButton, settingsButton);
    }

}
