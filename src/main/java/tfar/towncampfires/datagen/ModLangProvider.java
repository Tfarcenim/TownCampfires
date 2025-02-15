package tfar.towncampfires.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;
import tfar.towncampfires.TownCampfires;

public class ModLangProvider extends LanguageProvider {
    public ModLangProvider(DataGenerator gen, String locale) {
        super(gen, TownCampfires.MODID, locale);
    }

    @Override
    protected void addTranslations() {

    }
}
