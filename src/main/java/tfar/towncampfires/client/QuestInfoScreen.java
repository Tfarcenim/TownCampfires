package tfar.towncampfires.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import tfar.towncampfires.data.quest.Quest;
import tfar.towncampfires.data.quest.QuestCriteria;

import java.util.List;

public class QuestInfoScreen extends BasicScreen {
    private final TownCampfireScreen parent;
    private final Quest quest;

    protected QuestInfoScreen(Component pTitle, TownCampfireScreen parent, Quest quest) {
        super(pTitle);
        this.parent = parent;
        this.quest = quest;
    }

    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        int xSize = imageWidth;
        int ySize = imageHeight;
        RenderUtils.blitNineSlicedSized(pPoseStack, BACKGROUND, leftPos, topPos,
                xSize, ySize, 4, 4, 12, 12, 0, 0, 12, 12);
    }

    @Override
    public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderLabels(pPoseStack, pMouseX, pMouseY);
        this.font.draw(pPoseStack, this.title, (float) this.titleLabelX + leftPos, (float) this.titleLabelY + topPos, 0xffffff);

        int spacing = 12;

        int xStart = leftPos + 6;
        int yLine = topPos + titleLabelY;
        itemRenderer.renderAndDecorateFakeItem(quest.icon(), xStart + font.width(title)+2, yLine-4);

        List<FormattedCharSequence> seq = quest.desc().stream().map(Component::getVisualOrderText).toList();

        yLine += spacing;

        for (int i = 0; i < seq.size(); i++) {
            FormattedCharSequence formattedCharSequence = seq.get(i);
            font.draw(pPoseStack, formattedCharSequence, xStart + 1, yLine, 0xffffff);
            yLine += spacing;
        }
        font.draw(pPoseStack, Component.literal("Complete Conditions"), xStart, yLine, TownCampfireScreen.DARK_GRAY);
        yLine += spacing;
        List<Pair<QuestCriteria<?>, Integer>> criterias = quest.criterias();
        for (int i = 0; i < criterias.size(); i++) {
            Pair<QuestCriteria<?>, Integer> entry = criterias.get(i);
            QuestCriteria<?> criteria = entry.getFirst();
            int count = entry.getSecond();
            font.draw(pPoseStack, criteria.desc().copy().append(" " + count), xStart, yLine, 0xffffff);
            yLine += spacing;
        }

        font.draw(pPoseStack, Component.literal("Fail Conditions"), xStart, yLine, TownCampfireScreen.DARK_GRAY);
        yLine += spacing;

        List<Pair<QuestCriteria<?>, Integer>> failCriterias = quest.failureCriterias();
        for (int i = 0; i < failCriterias.size(); i++) {
            Pair<QuestCriteria<?>, Integer> entry = failCriterias.get(i);
            QuestCriteria<?> criteria = entry.getFirst();
            int count = entry.getSecond();
            font.draw(pPoseStack, criteria.desc().copy().append(count > 1 ? " " + count : ""), xStart, yLine, 0xffffff);
            yLine += spacing;
        }
    }
}
