package fewizz.canpipe.fabric;

import net.fabricmc.loader.api.FabricLoader;

public class PlatformHelperService extends fewizz.canpipe.PlatformHelperService {

    @Override
    public boolean impl_isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

}
