package fewizz.canpipe.neoforge;

import net.neoforged.fml.ModList;

public class PlatformHelperService extends fewizz.canpipe.PlatformHelperService {

    @Override
    public boolean impl_isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
    
}
