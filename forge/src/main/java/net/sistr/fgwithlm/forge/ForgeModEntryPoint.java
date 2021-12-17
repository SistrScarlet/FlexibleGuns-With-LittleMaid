package net.sistr.fgwithlm.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.sistr.fgwithlm.FGWithLMMod;
import thedarkcolour.kotlinforforge.forge.ForgeKt;

@Mod(FGWithLMMod.MODID)
public class ForgeModEntryPoint {

    public ForgeModEntryPoint() {
        EventBuses.registerModEventBus(FGWithLMMod.MODID, ForgeKt.getMOD_BUS());
        FGWithLMMod.init();
    }
}
