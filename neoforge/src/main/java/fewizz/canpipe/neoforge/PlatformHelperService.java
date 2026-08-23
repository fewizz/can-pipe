package fewizz.canpipe.neoforge;

import net.neoforged.fml.loading.FMLLoader;

public class PlatformHelperService extends fewizz.canpipe.PlatformHelperService {

    @Override
    public boolean impl_isModLoaded(String modId) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
    }

}
