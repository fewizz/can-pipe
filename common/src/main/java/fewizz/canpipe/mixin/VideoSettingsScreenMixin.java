package fewizz.canpipe.mixin;

import java.util.ArrayList;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import blue.endless.jankson.JsonObject;
import fewizz.canpipe.CanPipe;
import fewizz.canpipe.pipeline.Pipelines;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
        var leftButton = new OptionInstance<>(
            "Pipeline",
            OptionInstance.noTooltip(),
            (Component component, ResourceLocation loc) -> {
                if (loc.getPath().equals("")) {
                    return Component.literal("No");
                }
                JsonObject p = Pipelines.RAW_PIPELINES.get(loc);
                return Component.translatable(p.get(String.class, "nameKey"));
            },
            new OptionInstance.LazyEnum<ResourceLocation>(
                () -> {
                    var values = new ArrayList<ResourceLocation>();
                    values.add(ResourceLocation.withDefaultNamespace(""));
                    for (var p : Pipelines.RAW_PIPELINES.keySet()) {
                        values.add(p);
                    }
                    return values;
                },
                (ResourceLocation val) -> {
                    return Optional.of(val);
                },
                null
            ),
            Pipelines.getCurrent() == null ? ResourceLocation.withDefaultNamespace("") : Pipelines.getCurrent().location,
            (ResourceLocation p) -> {
                try {
                    Pipelines.loadAndSetPipeline(p.getPath().equals("") ? null : p);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        ).createButton(this.options);

        leftButton.setWidth(leftButton.getWidth() + 130);

        var rightButton = new Button(0, 0, 20, 20, Component.literal("*"), (b) -> {}, null) {
            public void setX(int x) {
                super.setX(x + 130);
            };
        };

        list.addSmall(leftButton, rightButton);
    }

}
