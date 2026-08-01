package fewizz.canpipe.mixin.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen implements VideoSettingsScreenExtended {

    VideoSettingsScreenMixin() { super(null, null, null); }

    @Unique private Button canpipe_pipelineSettingsButton = null;
    @Unique private CycleButton<Optional<PipelineRaw>> canpipe_pipelineSwitchButton = null;
    @Unique private ImageWidget canpipe_pipelineWarningSign = null;

    @Override
    public void canpipe_onPipelineLoaded() {
        assert this.list != null;

        Pipeline pipeline = Pipelines.getCurrent();
        PipelineRaw rawPipeline = Pipelines.getCurrentRaw();

        boolean showPipelineSettingsButton = pipeline != null && !rawPipeline.options.isEmpty();

        MutableComponent warningSignComponent = null;
        if (rawPipeline != null && !rawPipeline.awareOfDepthRangeChanges) {
            warningSignComponent = Component.literal("This pipeline isn't aware of depth range changes.\nHere be dragons.");
        }

        if (warningSignComponent != null) {
            this.canpipe_pipelineWarningSign.setTooltip(Tooltip.create(warningSignComponent));
        }
        boolean showWarningSign = warningSignComponent != null;

        int switchButtonWidth = Button.DEFAULT_WIDTH + 10 + Button.DEFAULT_WIDTH;
        boolean switchButtonIsShortened = false;
        if (showPipelineSettingsButton) { switchButtonWidth -= 20; switchButtonIsShortened = true; }
        if (showWarningSign) { switchButtonWidth -= 20; switchButtonIsShortened = true; }
        if (switchButtonIsShortened) { switchButtonWidth -= 10; }

        this.canpipe_pipelineSwitchButton.setWidth(switchButtonWidth);
        this.canpipe_pipelineSwitchButton.setValue(Optional.ofNullable(rawPipeline));
        this.canpipe_pipelineSettingsButton.visible = showPipelineSettingsButton;
        this.canpipe_pipelineWarningSign.visible = showWarningSign;

        @SuppressWarnings("unchecked") var textureFilteringButton = (CycleButton<TextureFilteringMethod>) this.list.findOption(this.options.textureFiltering());

        if (textureFilteringButton != null) {
            if (pipeline != null) {
                textureFilteringButton.active = false;
                textureFilteringButton.setValue(TextureFilteringMethod.NONE);
            }
            else {
                textureFilteringButton.active = true;
                textureFilteringButton.setValue(this.options.textureFiltering().get());
            }
        }

        @SuppressWarnings("unchecked") var improvedTransparencyButton = (CycleButton<Boolean>) this.list.findOption(this.options.improvedTransparency());

        if (improvedTransparencyButton != null) {
            if (pipeline != null) {
                improvedTransparencyButton.active = false;
                improvedTransparencyButton.setValue(pipeline.fabulousTargets != null);
            }
            else {
                improvedTransparencyButton.active = true;
                improvedTransparencyButton.setValue(this.options.improvedTransparency().get());
            }
        }

        @SuppressWarnings("unchecked") var entityShadowsButton = (CycleButton<Boolean>) this.list.findOption(this.options.entityShadows());

        if (entityShadowsButton != null) {
            if (pipeline != null && pipeline.shadows != null) {
                entityShadowsButton.active = false;
                entityShadowsButton.setValue(false);
            }
            else {
                entityShadowsButton.active = true;
                entityShadowsButton.setValue(this.options.entityShadows().get());
            }
        }
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
        var settingsButton = new Button.Plain(
            0, 0,
            20, 20,
            Component.literal("S"),
            (button) -> {
                Pipeline current = Pipelines.getCurrent();
                if (current == null) {
                    return;
                }
                this.minecraft.gui.setScreen(new PipelineOptionsScreen(
                    (Screen) (Object) this,
                    Pipelines.RAW_PIPELINES.get(current.location),
                    current.appliedOptions
                ));
            },
            Supplier::get
        ) {};

        var pipelineButton = (CycleButton<Optional<PipelineRaw>>) new OptionInstance<Optional<PipelineRaw>>(
            "Pipeline",
            (Optional<PipelineRaw> p) -> {  // widget tooltip
                var e = Pipelines.getLoadingError();
                if (e == null) return null;
                return Tooltip.create(Component.literal(
                    e.getMessage() != null ? e.getMessage() : e.toString()
                ));
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
                            .map(Optional::of)
                            .toList()
                    );
                    return values;
                },
                Optional::of,
                null
            ),
            Optional.ofNullable(Pipelines.getCurrentRaw()),  // current widget value
            (Optional<PipelineRaw> p) -> {  // on widget value changed
                if (Pipelines.getCurrentRaw() == p.orElse(null)) return;

                Pipelines.loadAndSetPipeline(p.orElse(null), null);
                // update value to update label
                // first check is to avoid recursive pipeline loading
                canpipe_onPipelineLoaded();
            }
        ).createButton(this.options);

        this.canpipe_pipelineSettingsButton = settingsButton;
        this.canpipe_pipelineSwitchButton = pipelineButton;

        this.canpipe_pipelineWarningSign = ImageWidget.sprite(20, 20, Identifier.withDefaultNamespace("icon/info"));
        this.canpipe_pipelineWarningSign.setTooltip(Tooltip.create(Component.literal(":)")));

        ((AbstractSelectionListAccessor) this.list).callAddEntry(new OptionsList.AbstractEntry() {

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(
                    VideoSettingsScreenMixin.this.canpipe_pipelineSwitchButton,
                    VideoSettingsScreenMixin.this.canpipe_pipelineWarningSign,
                    VideoSettingsScreenMixin.this.canpipe_pipelineSettingsButton
                );
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(
                    VideoSettingsScreenMixin.this.canpipe_pipelineSwitchButton,
                    VideoSettingsScreenMixin.this.canpipe_pipelineWarningSign,
                    VideoSettingsScreenMixin.this.canpipe_pipelineSettingsButton
                );
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                final int left = VideoSettingsScreenMixin.this.width / 2 - 155;
                VideoSettingsScreenMixin.this.canpipe_pipelineSwitchButton.setPosition(left, this.getContentY());
                VideoSettingsScreenMixin.this.canpipe_pipelineSwitchButton.extractRenderState(graphics, mouseX, mouseY, a);

                VideoSettingsScreenMixin.this.canpipe_pipelineWarningSign.setPosition(left + 265, this.getContentY());
                VideoSettingsScreenMixin.this.canpipe_pipelineWarningSign.extractRenderState(graphics, mouseX, mouseY, a);

                VideoSettingsScreenMixin.this.canpipe_pipelineSettingsButton.setPosition(left + 290, this.getContentY());
                VideoSettingsScreenMixin.this.canpipe_pipelineSettingsButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

        });
    }

    @Inject(
        method = "addOptions",
        at = @At("RETURN")
    )
    void onAllOptionsAdded(CallbackInfo ci) {
        this.canpipe_onPipelineLoaded();
    }

}
