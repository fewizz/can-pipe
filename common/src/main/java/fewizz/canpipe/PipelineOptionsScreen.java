package fewizz.canpipe;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import fewizz.canpipe.mixininterface.VideoSettingsScreenExtended;
import fewizz.canpipe.pipeline.OptionGroup;
import fewizz.canpipe.pipeline.PipelineRaw;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class PipelineOptionsScreen extends OptionsSubScreen {

    private final PipelineRaw raw;
    private final Map<OptionGroup.Element<?>, Object> appliedOptions;
    private PipelineOptionsList list;

    public PipelineOptionsScreen(
        Screen previousScreen,
        PipelineRaw raw,
        Map<OptionGroup.Element<?>, Object> appliedOptions
    ) {
        super(previousScreen, Minecraft.getInstance().options, Component.empty());
        this.raw = raw;
        this.appliedOptions = appliedOptions;
        this.layout.setHeaderHeight(20);
    }

    @Override
    protected void addTitle() {
        // nope
    }

    @Override
    protected void addContents() {
        this.list = this.layout.addToContents(
            new PipelineOptionsList(this, this.minecraft, this.raw, this.appliedOptions)
        );
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.list.updateSize(this.width, this.layout);
    }

    @Override
    public void extractBackground(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        if (this.minecraft.level == null) {
            super.extractBackground(graphics, mouseX, mouseY, a);
        }
    }

    @Override
    protected void addOptions() {}

    private class PipelineOptionsList extends ContainerObjectSelectionList<PipelineOptionsList.Entry> {

        public PipelineOptionsList(
            PipelineOptionsScreen screen,
            Minecraft minecraft,
            PipelineRaw raw,
            Map<OptionGroup.Element<?>, Object> appliedOptions
        ) {
            super(minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 18);

            for (OptionGroup o : screen.raw.options.values()) {
                this.addEntry(new CategoryEntry(Component.empty()));
                this.addEntry(new CategoryEntry(Component.translatable(o.categoryKey())));

                for (OptionGroup.Element<?> e : o.elements().values()) {
                    Consumer<Object> applyOptionValue = (@Nullable Object value) -> {
                        Pipelines.loadAndSetPipeline(raw, Pair.of(e, value));
                        if (
                            minecraft.gui.screen() instanceof PipelineOptionsScreen &&
                            Pipelines.getCurrent() == null
                        ) {
                            if (lastScreen instanceof VideoSettingsScreenExtended vss) {
                                vss.canpipe_onPipelineLoaded();
                            }
                            onClose();
                        }
                    };
                    this.addEntry(new OptionEntry(e, appliedOptions.get(e), applyOptionValue));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected void extractListBackground(final @NonNull GuiGraphicsExtractor graphics) {}

        @Override
        protected void extractScrollbar(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
            if (this.scrollable()) {
                graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    Identifier.withDefaultNamespace("widget/scroller"),
                    this.scrollBarX(),
                    this.scrollBarY(),
                    6,
                    this.scrollerHeight()
                );
            }
        }

        abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

        public class CategoryEntry extends Entry {
            final Component name;
            private final int width;

            public CategoryEntry(final Component name) {
                this.name = name;
                this.width = minecraft.font.width(this.name);
            }

            @Override
            public void extractContent(
                final GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, boolean hovered, float a
            ) {
                graphics.text(
                    minecraft.font,
                    this.name,
                    (PipelineOptionsList.this.width - this.width) / 2,
                    this.getY() + (this.getHeight() - minecraft.font.lineHeight) / 2 - 5,
                    0xFFFFFFFF
                );
            }

            @Override
            public ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
                return null;
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return Collections.emptyList();
            }
        }

        public class OptionEntry extends Entry {
            private final StringWidget nameWidget;
            private final AbstractWidget valueWidget;
            private final AbstractButton resetButton;
            private Runnable onReset;
            private boolean handlingReset = false;  // smells

            static final int RIGHT_SHIFT = 0;
            static final int BUTTON_WIDTH = Button.DEFAULT_WIDTH - 25 - RIGHT_SHIFT;
            static final int BUTTON_HEIGHT = 17;

            OptionEntry(
                OptionGroup.Element<?> e, Object appliedValue,
                Consumer<Object> applyValue
            ) {
                this.nameWidget = new StringWidget(Component.translatable(e.nameKey), minecraft.font);
                this.resetButton = new Button.Builder(Component.literal("←"), resetButton -> {
                    this.handlingReset = true;
                    this.onReset.run();
                    this.handlingReset = false;
                    resetButton.visible = false;
                })
                    .tooltip(Tooltip.create(Component.translatable("canpipe.button.reset")))
                    .size(20-4, BUTTON_HEIGHT-2)
                    .build();

                this.resetButton.visible = appliedValue != null;

                Consumer<Object> enableResetAndApplyValue = (Object value) -> {
                    boolean equalsToDefault = value != null ? value.equals(e.defaultValue) : e.defaultValue == null;
                    this.resetButton.visible = !equalsToDefault;
                    applyValue.accept((!this.handlingReset && !equalsToDefault) ? value : null);
                };

                if (e instanceof OptionGroup.BooleanElement boolElement) {
                    var initialValue = appliedValue != null ? boolElement.validate(appliedValue) : boolElement.defaultValue;

                    var widget = Checkbox.builder(Component.empty(), minecraft.font)
                        .selected(initialValue)
                        .onValueChange((checkbox, state) -> {
                            enableResetAndApplyValue.accept(state);
                        })
                        .build();

                    this.valueWidget = widget;
                    this.onReset = () -> {
                        if (widget.selected() != boolElement.defaultValue) {
                            widget.onPress(null);
                        }
                        else {
                            enableResetAndApplyValue.accept(null);
                        }
                    };
                }
                else if (e instanceof OptionGroup.FloatElement floatElement) {
                    NumberFormat numberFormat = NumberFormat.getInstance();
                    numberFormat.setMinimumFractionDigits(1);
                    numberFormat.setMaximumFractionDigits(3);

                    Function<Double, Component> valueToComponent = (Double v) -> {
                        return Component.literal(numberFormat.format(v));
                    };

                    var initialValue = appliedValue != null ? (double) appliedValue : floatElement.defaultValue;

                    var widget = new AbstractSliderButton(
                        0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                        valueToComponent.apply(initialValue),
                        (initialValue - floatElement.min) / (floatElement.max - floatElement.min)
                    ) {
                        @Override
                        protected void updateMessage() {
                            this.setMessage(valueToComponent.apply(
                                (this.value * (floatElement.max - floatElement.min)) + floatElement.min
                            ));
                        }

                        @Override
                        protected void applyValue() {}

                        @Override
                        public void onRelease(@NonNull MouseButtonEvent e) {
                            super.onRelease(e);
                            var value = (this.value * (floatElement.max - floatElement.min)) + floatElement.min;
                            enableResetAndApplyValue.accept(value);
                        }

                        @Override public void setValue(double newValue) { super.setValue(newValue); }
                    };

                    this.valueWidget = widget;
                    this.onReset = () -> {
                        widget.setValue((floatElement.defaultValue - floatElement.min) / (floatElement.max - floatElement.min));
                        enableResetAndApplyValue.accept(null);
                    };
                }
                else if (e instanceof OptionGroup.IntegerElement intElement) {
                    Function<Long, Component> valueToComponent = (Long v) -> {
                        return Component.literal(Long.toString(v));
                    };

                    var initialValue = appliedValue != null ? (long) appliedValue : intElement.defaultValue;

                    var widget = new AbstractSliderButton(
                        0, 0, BUTTON_WIDTH, BUTTON_HEIGHT,
                        valueToComponent.apply(initialValue),
                        (double)(initialValue - intElement.min) / (double)(intElement.max - intElement.min)
                    ) {
                        @Override
                        protected void updateMessage() {
                            setMessage(valueToComponent.apply(
                                (long) ((this.value * (intElement.max - intElement.min)) + intElement.min)
                            ));
                        }

                        @Override
                        protected void applyValue() {}

                        @Override
                        public void onRelease(@NonNull MouseButtonEvent e) {
                            super.onRelease(e);
                            var value = (long)((this.value * (intElement.max - intElement.min)) + intElement.min);
                            enableResetAndApplyValue.accept(value);
                        }

                        @Override public void setValue(double newValue) { super.setValue(newValue); }

                    };

                    this.valueWidget = widget;

                    this.onReset = () -> {
                        widget.setValue((double)(intElement.defaultValue - intElement.min) / (double)(intElement.max - intElement.min));
                        enableResetAndApplyValue.accept(null);
                    };
                }
                else if (e instanceof OptionGroup.EnumElement enumElement) {
                    var widget = CycleButton.builder(
                        (String s) -> Component.literal(
                            (s.substring(0, 1).toUpperCase() + s.substring(1)).replace("_", " ")
                        ),
                        appliedValue != null ? (String) appliedValue : enumElement.defaultValue
                    )
                        .withValues(enumElement.choices)
                        .displayOnlyValue()
                        .create(
                            0, 0,
                            BUTTON_WIDTH, BUTTON_HEIGHT,
                            Component.empty(),  // no name needed
                            (CycleButton<String> button, String choice) -> {
                                enableResetAndApplyValue.accept(choice);
                            }
                        );

                    this.valueWidget = widget;

                    this.onReset = () -> {
                        widget.setValue(enumElement.defaultValue);
                        enableResetAndApplyValue.accept(null);
                    };
                }
                else {
                    throw new NotImplementedException();
                }

                this.nameWidget.setTooltip(
                    e.descriptionKey != null
                    ? Tooltip.create(Component.translatable(e.descriptionKey))
                    : null
                );
            }

            @Override
            public void extractContent(
                final @NonNull GuiGraphicsExtractor graphics,
                int mouseX, int mouseY, boolean hovered, float a
            ) {
                this.nameWidget.setPosition(
                    PipelineOptionsList.this.width / 2 - this.nameWidget.getWidth() - 5 + RIGHT_SHIFT,
                    this.getY() + (this.getHeight() - minecraft.font.lineHeight) / 2
                );
                this.nameWidget.extractRenderState(graphics, mouseX, mouseY, a);

                this.valueWidget.setPosition(
                    PipelineOptionsList.this.width / 2 + 5 + RIGHT_SHIFT,
                    this.getY() + (this.getHeight() - this.valueWidget.getHeight()) / 2
                );
                this.valueWidget.extractRenderState(graphics, mouseX, mouseY, a);

                this.resetButton.setPosition(
                    PipelineOptionsList.this.width / 2 + 5 + RIGHT_SHIFT + BUTTON_WIDTH + 5,
                    this.getY() + (this.getHeight() - this.valueWidget.getHeight()) / 2 + 1
                );
                this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);
            }

            @Override
            public @NonNull List<? extends GuiEventListener> children() {
                return List.of(this.valueWidget, this.resetButton);
            }

            @Override
            public @NonNull List<? extends NarratableEntry> narratables() {
                return ImmutableList.of(this.valueWidget, this.resetButton);
            }
        }
    }

}
