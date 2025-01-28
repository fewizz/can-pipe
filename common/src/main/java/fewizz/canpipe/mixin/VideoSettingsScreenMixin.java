package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fewizz.canpipe.PipelineOptionsScreen;
import fewizz.canpipe.pipeline.Pipeline;
import fewizz.canpipe.pipeline.PipelineRaw;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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

        var rightButton = new Button(
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

        Pipeline current = Pipelines.getCurrent();

        var leftButton = new OptionInstance<Optional<PipelineRaw>>(
            "Pipeline",
            OptionInstance.noTooltip(),
            (Component component, Optional<PipelineRaw> p) -> {
                if (p.isEmpty()) {
                    return Component.literal("No");
                }
                return Component.translatable(p.get().nameKey);
            },
            new OptionInstance.LazyEnum<Optional<PipelineRaw>>(
                () -> {
                    var values = new ArrayList<Optional<PipelineRaw>>();
                    values.add(Optional.empty());
                    values.addAll(Pipelines.RAW_PIPELINES.values().stream().map(p -> Optional.of(p)).collect(Collectors.toList()));
                    return values;
                },
                (Optional<PipelineRaw> val) -> Optional.of(val),
                null
            ),
            Optional.ofNullable(current == null ? null : Pipelines.RAW_PIPELINES.get(current.location)),
            (Optional<PipelineRaw> p) -> {
                if (!Pipelines.loadAndSetPipeline(p.orElse(null), Map.of())) {
                    // ...
                }
            }
        ).createButton(this.options);

        leftButton.setWidth(leftButton.getWidth() + 130);

        list.addSmall(leftButton, rightButton);
    }

}
