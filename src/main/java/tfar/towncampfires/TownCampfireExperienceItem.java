package tfar.towncampfires;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TownCampfireExperienceItem extends Item {

    public static final String REWARD = TownCampfires.MODID+":reward";
    public static final List<Long> defaultRewards = List.of(1L,2L,5L,10L,20L,50L,100L,500L,1000L,5000L,10000L);

    public TownCampfireExperienceItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        long reward = getReward(pStack);
        if (reward > 0) {
            pTooltipComponents.add(Component.literal("Town Campfire Experience +"+reward+" when used"));
        }
    }

    public static long getReward(ItemStack stack){
        return stack.hasTag() ? stack.getTag().getLong(REWARD) : 0;
    }

    @Override
    public void fillItemCategory(CreativeModeTab pCategory, NonNullList<ItemStack> pItems) {
        if (this.allowedIn(pCategory)) {
            for (long l : defaultRewards) {
                ItemStack stack = new ItemStack(this);
                stack.getOrCreateTag().putLong(REWARD,l);
                pItems.add(stack);
            }
        }
    }
}
