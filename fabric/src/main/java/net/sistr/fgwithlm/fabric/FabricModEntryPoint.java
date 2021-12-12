package net.sistr.fgwithlm.fabric;

import net.fabricmc.api.ModInitializer;
import net.sistr.fgwithlm.FGWithLMMod;

public class FabricModEntryPoint implements ModInitializer {

    @Override
    public void onInitialize() {
        FGWithLMMod.init();
    }

}
