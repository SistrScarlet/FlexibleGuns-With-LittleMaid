package net.sistr.fgwithlm;

import net.minecraft.util.Identifier;
import net.sistr.fgwithlm.mode.FlexibleGunMode;
import net.sistr.flexibleguns.item.GunItem;
import net.sistr.littlemaidrebirth.api.mode.ItemMatchers;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.api.mode.ModeType;

public class FGWithLMMod {
    public static final String MODID = "fgwithlm";
    public static final ModeType<FlexibleGunMode> FLEXIBLE_GUN_MODE_TYPE = buildFlexibleGunMode().build();

    public static void init() {
        ModeManager.INSTANCE.register(new Identifier(MODID, "flexiblegun"), FLEXIBLE_GUN_MODE_TYPE);
    }

    public static ModeType.Builder<FlexibleGunMode> buildFlexibleGunMode() {
        return ModeType.<FlexibleGunMode>builder((type, maid) ->
                new FlexibleGunMode(type, "FlexibleGunner", maid, 20))
                .addItemMatcher(ItemMatchers.clazz(GunItem.class));
    }

}
