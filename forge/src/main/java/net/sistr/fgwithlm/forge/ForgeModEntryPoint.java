package net.sistr.fgwithlm.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sistr.fgwithlm.FGWithLMMod;

public class ForgeModEntryPoint {

    public ForgeModEntryPoint() {
        EventBuses.registerModEventBus(FGWithLMMod.MODID, FMLJavaModLoadingContext.get().getModEventBus());
        FGWithLMMod.init();
    }
}
