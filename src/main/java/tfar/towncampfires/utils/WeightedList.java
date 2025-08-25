package tfar.towncampfires.utils;

import java.util.ArrayList;
import java.util.Random;

public class WeightedList<E extends WeightedList.Entry> extends ArrayList<E> {


    final ArrayList<E> backingList;
    long totalWeight;

    public WeightedList(ArrayList<E> list) {
        this.backingList = list;
        totalWeight = getTotalWeight();
    }

    public static <E extends Entry> WeightedList<E> create() {
        return new WeightedList<>(new ArrayList<>());
    }

    @Override
    public boolean add(E e) {
        boolean add = backingList.add(e);
        totalWeight = getTotalWeight();
        return add;
    }

    public boolean remove(E e) {
        boolean remove = backingList.remove(e);
        totalWeight = getTotalWeight();
        return remove;
    }

    public E remove(int index) {
        E remove = backingList.remove(index);
        totalWeight = getTotalWeight();
        return remove;
    }

    @Override
    public E get(int index) {
        return backingList.get(index);
    }

    @Override
    public int size() {
        return backingList.size();
    }

    public E getRandom(Random pRandom,boolean withReplacement) {
        if (this.totalWeight == 0) {
            return null;
        } else {
            long i = pRandom.nextLong(this.totalWeight);
            E e = getWeightedItem(i);
            if (!withReplacement) {
                remove(e);
            }
            return e;
        }
    }

    public E getWeightedItem(long pWeightedIndex) {
        for(E e : backingList) {
            pWeightedIndex -= e.weight();
            if (pWeightedIndex < 0) {
                return e;
            }
        }
        return null;
    }


    long getTotalWeight() {
        return backingList.stream().mapToLong(Entry::weight).sum();
    }


    public interface Entry {
        long weight();
    }

    public record Wrapper<T>(T data,long weight) implements Entry {
        public static <T> Wrapper<T> wrap(T pData, long pWeight) {
            return new Wrapper<>(pData, pWeight);
        }
    }
}
