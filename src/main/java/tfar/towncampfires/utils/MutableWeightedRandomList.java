package tfar.towncampfires.utils;

import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MutableWeightedRandomList<E extends WeightedEntry> {
    private int totalWeight;
    private final List<E> items;

    public MutableWeightedRandomList(List<? extends E> pItems) {
        this.items = new ArrayList<>(pItems);
        this.totalWeight = WeightedRandom.getTotalWeight(pItems);
    }

    public static <E extends WeightedEntry> MutableWeightedRandomList<E> create() {
        return new MutableWeightedRandomList<>(List.of());
    }

    public boolean add(E e) {
        totalWeight+=e.getWeight().asInt();
        return items.add(e);
    }

    public boolean remove(E e) {
        totalWeight-=e.getWeight().asInt();
        return items.remove(e);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public Optional<E> getRandom(RandomSource pRandom) {
        if (this.totalWeight == 0) {
            return Optional.empty();
        } else {
            int i = pRandom.nextInt(this.totalWeight);
            return WeightedRandom.getWeightedItem(this.items, i);
        }
    }

    public List<E> unwrap() {
        return this.items;
    }
}