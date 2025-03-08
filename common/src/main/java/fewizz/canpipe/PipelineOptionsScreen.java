package fewizz.canpipe;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.ImmutableList;

import fewizz.canpipe.mixininterface.VideoSettingsScreenExtended;
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
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class PipelineOptionsScreen extends OptionsSubScreen {

    private final PipelineRaw raw;
    private final Map<Option.Element<?>, Object> appliedOptions;
    private PipelineOptionsList list;

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
            super(minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), 18);

            for (Option o : screen.raw.options.values()) {
                this.addEntry(new CategoryEntry(Component.empty()));
                this.addEntry(new CategoryEntry(Component.translatable(o.categoryKey)));

                for (Option.Element<?> e : o.elements.values()) {
                    Consumer<Object> applyOptionValue = (Object value) -> {
                        Pipelines.loadAndSetPipeline(raw, Map.of(e, value));
                        if (
                            minecraft.screen instanceof PipelineOptionsScreen &&
                            Pipelines.getCurrent() == null
                        ) {
                            if (lastScreen instanceof VideoSettingsScreenExtended vss) {
                                vss.canpipe_onPipelineLoaded();
                            }
                            onClose();
                        }
                    };
                    this.addEntry(new OptionEntry(raw, e, appliedOptions.get(e), applyOptionValue));
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 310;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {}

        @Override
        protected void renderScrollbar(GuiGraphics guiGraphics) {
            if (this.scrollbarVisible()) {
                guiGraphics.blitSprite(
                    RenderType::guiTextured,
                    ResourceLocation.withDefaultNamespace("widget/scroller"),
                    this.scrollBarX(),
                    this.scrollBarY(),
                    6,
                    this.scrollerHeight()
                );
            }
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
            public void render(
                GuiGraphics guiGraphics, int index,
                int top, int left,
                int width, int height,
                int mouseX, int mouseY,
                boolean hovering, float partialTick
            ) {
                guiGraphics.drawString(
                    minecraft.font,
                    this.name,
                    (PipelineOptionsList.this.width - this.width) / 2,
                    top + (height - minecraft.font.lineHeight) / 2 - 5,
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
            private final StringWidget nameWidget;
            private final AbstractWidget valueWidget;

            static final int RIGHT_SHIFT = 0;
            static final int BUTTON_WIDTH = Button.DEFAULT_WIDTH - 25 - RIGHT_SHIFT;
            static final int BUTTON_HEIGHT = 17;

            OptionEntry(
                PipelineRaw raw, Option.Element<?> e, Object appliedValue,
                Consumer<Object> applyValue
            ) {
                this.nameWidget = new StringWidget(Component.translatable(e.nameKey), minecraft.font);

                if (e instanceof Option.BooleanElement boolElement) {
                    var initialValue = appliedValue != null ? boolElement.validate(appliedValue) : boolElement.defaultValue;
                    this.valueWidget = Checkbox.builder(Component.empty(), minecraft.font)
                        .selected(initialValue)
                        .onValueChange((checkbox, state) -> {
                            applyValue.accept(state);
                        })
                        .build();
                }
                else if (e instanceof Option.FloatElement floatElement) {
                    NumberFormat numberFormat = NumberFormat.getInstance();
                    numberFormat.setMinimumFractionDigits(1);
                    numberFormat.setMaximumFractionDigits(3);

                    Function<Double, Component> valueToComponent = (Double v) -> {
                        return Component.literal(numberFormat.format(v));
                    };

                    var initialValue = appliedValue != null ? (double) appliedValue : floatElement.defaultValue;

                    this.valueWidget = new AbstractSliderButton(
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
                        public void onRelease(double mouseX, double mouseY) {
                            super.onRelease(mouseX, mouseY);
                            var value = (this.value * (floatElement.max - floatElement.min)) + floatElement.min;
                            applyValue.accept(value);
                        }
                    };
                }
                else if (e instanceof Option.IntegerElement intElement) {
                    Function<Long, Component> valueToComponent = (Long v) -> {
                        return Component.literal(Long.toString(v));
                    };

                    var initialValue = appliedValue != null ? (long) appliedValue : intElement.defaultValue;

                    this.valueWidget = new AbstractSliderButton(
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
                        public void onRelease(double mouseX, double mouseY) {
                            super.onRelease(mouseX, mouseY);
                            var value = (long)((this.value * (intElement.max - intElement.min)) + intElement.min);
                            applyValue.accept(value);
                        }
                    };
                }
                else if (e instanceof Option.EnumElement enumElement) {
                    this.valueWidget = CycleButton.builder((String s) -> Component.literal(
                        (s.substring(0, 1).toUpperCase() + s.substring(1)).replace("_", " ")
                    ))
                        .withValues(enumElement.choices)
                        .withInitialValue(appliedValue != null ? (String) appliedValue : enumElement.defaultValue)
                        .displayOnlyValue()
                        .create(
                            0, 0,
                            BUTTON_WIDTH, BUTTON_HEIGHT,
                            Component.empty(),  // no name needed
                            (CycleButton<String> button, String choice) -> {
                                applyValue.accept(choice);
                            }
                        );
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
            public void render(
                GuiGraphics guiGraphics, int index,
                int top, int left,
                int width, int height,
                int mouseX, int mouseY,
                boolean hovering, float partialTick
            ) {
                this.nameWidget.setPosition(
                    PipelineOptionsList.this.width / 2 - this.nameWidget.getWidth() - 5 + RIGHT_SHIFT,
                    top + (height - minecraft.font.lineHeight) / 2
                );
                this.nameWidget.render(guiGraphics, mouseX, mouseY, partialTick);

                this.valueWidget.setPosition(
                    PipelineOptionsList.this.width / 2 + 5 + RIGHT_SHIFT,
                    top + (height - this.valueWidget.getHeight()) / 2
                );
                this.valueWidget.render(guiGraphics, mouseX, mouseY, partialTick);
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(this.valueWidget);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return ImmutableList.of(this.valueWidget);
            }
        }
    }

}
