package tfar.towncampfires.config;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import tfar.towncampfires.utils.MiscCodecs;

import java.util.ArrayList;
import java.util.List;

public class TownCampfireConfig {
    public static final Server CONFIG;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    public static class Server {
        public final ConfigHelper.ConfigObject<List<Component>> defaultNames;
        public final ForgeConfigSpec.IntValue radius;

        public final ForgeConfigSpec.IntValue base_villagers;
        public final ForgeConfigSpec.IntValue villagers_per_level;

        public final ForgeConfigSpec.IntValue base_allowed_blocks;
        public final ForgeConfigSpec.IntValue allowed_blocks_per_level;

        public final ForgeConfigSpec.IntValue base_allowed_workbenches;
        public final ForgeConfigSpec.IntValue allowed_workbenches_per_level;

        public final ForgeConfigSpec.LongValue refresh_timer;

        public final ForgeConfigSpec.IntValue experience_per_level;
        
        public final ConfigHelper.ConfigObject<List<RandomIntegerRange>> positive_effects;
        public final ConfigHelper.ConfigObject<List<RandomIntegerRange>> negative_effects;

        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("town_campfire_stats");
            defaultNames = ConfigHelper.defineObject(builder,"default_campfire_names", MiscCodecs.COMPONENT_CODEC.listOf(),defaultCampfireNames());
            radius = builder.defineInRange("radius",80,1,256);
            base_villagers = builder.defineInRange("base_villagers",7,1,1000);
            villagers_per_level = builder.defineInRange("villagers_per_level",5,1,1000);

            base_allowed_blocks = builder.defineInRange("base_allowed_blocks",50,1,10000000);
            allowed_blocks_per_level = builder.defineInRange("allowed_blocks_per_level",10,1,10000000);

            base_allowed_workbenches = builder.defineInRange("base_allowed_workbenches",1,1,1000000);
            allowed_workbenches_per_level = builder.defineInRange("allowed_workbenches_per_level",1,1,1000000);

            refresh_timer = builder.defineInRange("refresh_timer",20 * 60 * 20 * 7L,20,10000000000000L);
            experience_per_level = builder.defineInRange("experience_per_level",5000,1,10000000);

            builder.push("effect_configuration");

            positive_effects = ConfigHelper.defineObject(builder,"positive",RandomIntegerRange.CODEC.listOf(),
                    List.of(new RandomIntegerRange(1,1,0)));

            negative_effects = ConfigHelper.defineObject(builder,"negative",RandomIntegerRange.CODEC.listOf(),
                    List.of(new RandomIntegerRange(0,0,0)));

            builder.pop();

            builder.pop();
        }

        static List<Component> defaultCampfireNames() {
            List<Component > names = new ArrayList<>();
            names.add(Component.literal("Tokyo"));
            names.add(Component.literal("Delhi"));
            names.add(Component.literal("Shanghai"));
            names.add(Component.literal("Bangladesh"));
            names.add(Component.literal("Cairo"));
            names.add(Component.literal("Sao Paulo"));
            names.add(Component.literal("Mexico City"));
            names.add(Component.literal("Beijing"));
            names.add(Component.literal("Mumbai"));
            names.add(Component.literal("Osaka"));
            return names;
        }

    }

}
