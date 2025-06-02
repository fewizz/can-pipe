package fewizz.canpipe;

import java.util.Collections;
import java.util.List;

import fewizz.canpipe.pipeline.PassBase;
import fewizz.canpipe.pipeline.Pipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PipelineIODebugScreen extends Screen {
    final Pipeline pipeline;

    public PipelineIODebugScreen(Pipeline p) {
        super(Component.literal("Pipeline IO Debug"));
        this.pipeline = p;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new PassesList(pipeline, minecraft, 140, height, 0, 16));
    }

    class PassesList extends AbstractSelectionList<PassesList.Entry> {

        public PassesList(Pipeline pipeline, Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            for (PassBase pass : pipeline.fabulousPasses) {
                this.addEntry(new Entry(pass.name));
            }
        }

        @Override
        public int getRowWidth() { return this.width; }

        class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            final String passName;

            Entry(String passName) {
                this.passName = passName;
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return Collections.emptyList();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return Collections.emptyList();
            }

            @Override
            public void render(
                GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX,
                int mouseY, boolean hovering, float partialTick
            ) {
                guiGraphics.drawString(font, Component.literal(this.passName), left+5, top, 0xFFFFFF);
            }

        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }

    }

}
