package fewizz.canpipe.compat.cinnabar.mixin;

import java.nio.file.Path;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.fabricmc.fabric.impl.resource.pack.ModNioPackResources;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.PackType;

// Cinnabar 1.21.11-0.0.7-beta-33-g407732e has outdated dynamictransforms.glsl file
// Won't be required for 26.x+
@Mixin(ModNioPackResources.class)
public class DontLoadCustomDynamictransformsMixin {

    @Final private String id;

    @Inject(
        method = "getPath",
        at = @At("HEAD"),
        cancellable = true
    )
    void getPath(String filename, CallbackInfoReturnable<Path> cir) {
        if (this.id.equals("cinnabar") && filename.endsWith("/dynamictransforms.glsl")) {
            System.out.println("SKIPPED!!!!");
            cir.setReturnValue(null);
            cir.cancel();
        }
    }

    @Inject(
        method = "listResources",
        at = @At("HEAD"),
        cancellable = true
    )
    public void listResources(PackType type, String namespace, String path, ResourceOutput visitor, CallbackInfo ci) {
        if (this.id.equals("cinnabar") && namespace.equals("minecraft")) {
            ci.cancel();
        }
    }
}
