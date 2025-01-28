package fewizz.canpipe;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import fewizz.canpipe.pipeline.Option;
import fewizz.canpipe.pipeline.PipelineRaw;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class PipelineOptionsScreen extends OptionsSubScreen {

    final PipelineRaw raw;
    final Map<Option.Element<?>, Object> appliedOptions;
    PipelineOptionsList list;

    public PipelineOptionsScreen(
        Screen previousScreen,
        PipelineRaw raw,
        Map<Option.Element<?>, Object> appliedOptions
    ) {
        super(previousScreen, Minecraft.getInstance().options, Component.empty());
        this.raw = raw;
        this.appliedOptions = appliedOptions;
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
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft.level == null) {
            super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void addOptions() {}

    private class PipelineOptionsList extends ContainerObjectSelectionList<PipelineOptionsList.Entry> {

        public PipelineOptionsList(
            PipelineOptionsScreen screen,
            Minecraft minecraft,
            PipelineRaw raw,
            Map<Option.Element<?>, Object> appliedOptions
        ) {
            super(minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 20);

            for (Option o : screen.raw.options.values()) {
                addEntry(new CategoryEntry(Component.translatable(o.categoryKey)));

                for (Option.Element<?> e : o.elements.values()) {
                    addEntry(new OptionEntry(raw, e, appliedOptions.get(e)));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
        }

        abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {}

        public class CategoryEntry extends Entry {
            final Component name;
            private final int width;

            public CategoryEntry(final Component name) {
                this.name = name;
                this.width = minecraft.font.width(this.name);
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(
                    minecraft.font,
                    this.name,
                    (PipelineOptionsList.this.width - this.width) / 2,
                    top + (height - minecraft.font.lineHeight) / 2,
                    0xFFFFFFFF
                );
            }

            @Override
            public ComponentPath nextFocusPath(FocusNavigationEvent event) {
                return null;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of();
            }
        }

        public class OptionEntry extends Entry {
            private final Component name;
            private final int nameWidth;
            private final AbstractWidget widget;

            static final int BUTTON_WIDTH = Button.DEFAULT_WIDTH - 25;

            OptionEntry(PipelineRaw raw, Option.Element<?> e, Object appliedValue) {
                this.name = Component.translatable(e.nameKey);
                this.nameWidth = minecraft.font.width(this.name);

                if (e instanceof Option.BooleanElement boolElement) {
                    var initialValue = appliedValue != null ? boolElement.validate(appliedValue) : boolElement.defaultValue;
                    this.widget = Checkbox.builder(Component.empty(), minecraft.font)
                        .selected(initialValue)
                        .onValueChange((checkbox, state) -> {
                            Pipelines.loadAndSetPipeline(raw, Map.of(e, state));
                        })
                        .build();
                }
                else if (e instanceof Option.FloatElement floatElement) {
                    Function<Double, Component> valueToComponent = (Double v) -> {
                        return Component.literal(Double.toString(v));
                    };

                    var initialValue = appliedValue != null ? (double) appliedValue : floatElement.defaultValue;

                    this.widget = new AbstractSliderButton(
                        0, 0, BUTTON_WIDTH, Button.DEFAULT_HEIGHT,
                        valueToComponent.apply(initialValue),
                        (initialValue - floatElement.min) / (floatElement.max - floatElement.min)
                    ) {

                        @Override
                        protected void updateMessage() {
                            setMessage(valueToComponent.apply((this.value * (floatElement.max - floatElement.min)) + floatElement.min));
                        }

                        @Override
                        protected void applyValue() {}

                        @Override
                        public void onRelease(double mouseX, double mouseY) {
                            super.onRelease(mouseX, mouseY);
                            var value = (this.value * (floatElement.max - floatElement.min)) + floatElement.min;
                            Pipelines.loadAndSetPipeline(raw, Map.of(e, value));
                        }
                    };
                }
                else if (e instanceof Option.IntegerElement intElement) {
                    Function<Long, Component> valueToComponent = (Long v) -> {
                        return Component.literal(Long.toString(v));
                    };

                    var initialValue = appliedValue != null ? (long) appliedValue : intElement.defaultValue;

                    this.widget = new AbstractSliderButton(
                        0, 0, BUTTON_WIDTH, Button.DEFAULT_HEIGHT,
                        valueToComponent.apply(initialValue),
                        (double)(initialValue - intElement.min) / (double)(intElement.max - intElement.min)
                    ) {
                        @Override
                        protected void updateMessage() {
                            setMessage(valueToComponent.apply((long)((this.value * (intElement.max - intElement.min)) + intElement.min)));
                        }

                        @Override
                        protected void applyValue() {}

                        @Override
                        public void onRelease(double mouseX, double mouseY) {
                            super.onRelease(mouseX, mouseY);
                            var value = (long)((this.value * (intElement.max - intElement.min)) + intElement.min);
                            Pipelines.loadAndSetPipeline(raw, Map.of(e, value));
                        }
                    };
                }
                else if (e instanceof Option.EnumElement enumElement) {
                    this.widget = CycleButton.builder((String s) -> Component.literal(s.substring(0, 1).toUpperCase() + s.substring(1)))
                        .withValues(enumElement.choices)
                        .withInitialValue(appliedValue != null ? (String) appliedValue : enumElement.defaultValue)
                        .displayOnlyValue()
                        .create(
                            0, 0,
                            BUTTON_WIDTH, Button.DEFAULT_HEIGHT,
                            null,  // no name needed
                            (CycleButton<String> button, String choice) -> {
                                Pipelines.loadAndSetPipeline(raw, Map.of(e, choice));
                            }
                        );
                }
                else {
                    this.widget = Button.builder(name, (b) -> {}).width(BUTTON_WIDTH).build();
                }
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(
                    minecraft.font,
                    this.name,
                    PipelineOptionsList.this.width / 2 - nameWidth - 5,
                    top + (height - minecraft.font.lineHeight) / 2,
                    0xFFFFFFFF
                );
                this.widget.setPosition(
                    PipelineOptionsList.this.width / 2 + 5,
                    top + (height - this.widget.getHeight()) / 2
                );
                this.widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.widget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return ImmutableList.of(this.widget);
            }
        }
    }

}
