package tfar.towncampfires.compat;

import net.minecraftforge.fml.ModList;

public enum LoadedMods {
    gamestages,tradingpost;
    public final boolean loaded = ModList.get().isLoaded(name());
}
