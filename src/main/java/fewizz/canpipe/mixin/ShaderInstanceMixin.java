package fewizz.canpipe.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import fewizz.canpipe.MockedProgram;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {

    @WrapOperation(
        method="<init>",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    ResourceLocation replaceResourceLocation(String name, Operation<ResourceLocation> op) {
        if ((Object) this instanceof MockedProgram) {
            name = name.replace(ShaderInstance.SHADER_PATH+"/core/", "");
            assert name.endsWith(".json");
            name = name.substring(0, name.length() - ".json".length());
            return ResourceLocation.parse(name);
        }
        return op.call(name);
    }

}
