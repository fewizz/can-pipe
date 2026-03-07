package fewizz.canpipe.compat.cinnabar;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import fewizz.canpipe.PlatformHelperService;

public class MixinPlugin implements IMixinConfigPlugin {

    private boolean isCinnabarLoaded = false;

    @Override
    public void onLoad(String mixinPackage) {
        this.isCinnabarLoaded = PlatformHelperService.isModLoaded("cinnabar");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.isCinnabarLoaded;
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

}
