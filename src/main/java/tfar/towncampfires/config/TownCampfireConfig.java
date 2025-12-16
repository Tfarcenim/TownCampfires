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

        public final ConfigHelper.ConfigObject<IntegerRange> starting_quests;

        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> levelup_walls;

        public final ForgeConfigSpec.IntValue slots_per_player;
        public final ForgeConfigSpec.IntValue trade_tab_level_requirement;

        public final ForgeConfigSpec.EnumValue<TeleportType> teleport_requirement;
        public final ForgeConfigSpec.ConfigValue<? extends String> teleport_item;
        public final ForgeConfigSpec.IntValue teleport_distance_per_item_cost;
        public final ForgeConfigSpec.IntValue teleport_minimum_cost;
        public final ForgeConfigSpec.IntValue teleport_maximum_cost;
        public final ForgeConfigSpec.IntValue unteleportable_distance;

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
            levelup_walls = builder.defineList("levelup_walls",() -> levelupWalls(),o -> o instanceof Integer integer && integer > 0);

            slots_per_player = builder.defineInRange("slots_per_player",10,1,Integer.MAX_VALUE);

            builder.push("effect_configuration");

            positive_effects = ConfigHelper.defineObject(builder,"positive",RandomIntegerRange.CODEC.listOf(),
                    List.of(new RandomIntegerRange(1,1,0)));

            negative_effects = ConfigHelper.defineObject(builder,"negative",RandomIntegerRange.CODEC.listOf(),
                    List.of(new RandomIntegerRange(0,0,0)));

            starting_quests = ConfigHelper.defineObject(builder,"starting_quests",IntegerRange.CODEC,new IntegerRange(1,5));
            trade_tab_level_requirement = builder.defineInRange("trade_tab_level_requirement",0,0,10000000);

            builder.pop();

            builder.push("teleport_requirements");
            teleport_requirement = builder.defineEnum("requires",TeleportType.EXPERIENCE);
            teleport_item = builder.define("item","minecraft:ender_pearl");
            teleport_distance_per_item_cost = builder.defineInRange("distance_per_item_cost",100,1,100000000);

            teleport_minimum_cost = builder.defineInRange("minimum_cost",100,1,100000000);
            teleport_maximum_cost = builder.defineInRange("maximum_cost",100,1,100000000);

            unteleportable_distance = builder.defineInRange("unteleportable_distance",1000000,1,100_000_000);

            //Added Campfire Type Multiplier: if the campfire type is different, add this cost to the math formula [static number]
            // Added Dimension Cost: added cost if the user is outside of the dimension they're teleporting to [dimension, multiplier]
            // Distance Multiplier 1: if current blocks surpass this number, add distance multiplier 1 instead of distance per item cost [blocks]
            //Cost Distance Multiplier 1: the cost of the teleport will be determined by by this number if distance multiplier 1 conditions are met. [new distance cost]
            //Distance Multiplier 2: if the current blocks surpass this number, add distance multiplier 2 instead of distance multiplier 1 and cost distance multiplier 1 [blocks]
            // Cost Distance Multiplier 2: the cost of the teleport will be determined by by this number if distance multiplier 2 conditions are met. [new distance cost]
            // Distance Multiplier 3: if the current blocks surpass this number, add distance multiplier 3 instead of the other distance multipliers.[blocks]
            // Cost Distance Multiplier 3: the cost of the teleport will be determined by by this number if distance multiplier 3 conditions are met.[new distance cost]
            // Unteleportable Distance: if the blocks exceed this distance, grey out the option of the teleport and disable it. [blocks]

            builder.pop();
            builder.pop();
        }

        static List<Integer> levelupWalls() {
            List<Integer> integers = new ArrayList<>();
            for (int i = 1; i < 20;i++) {
                integers.add(i * 5);
            }
            return integers;
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
