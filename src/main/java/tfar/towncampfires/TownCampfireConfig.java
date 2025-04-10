package tfar.towncampfires;

import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import tfar.towncampfires.utils.ConfigHelper;
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
        public Server(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            defaultNames = ConfigHelper.defineObject(builder,"default_campfire_names", MiscCodecs.COMPONENT_CODEC.listOf(),defaultCampfireNames());
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
