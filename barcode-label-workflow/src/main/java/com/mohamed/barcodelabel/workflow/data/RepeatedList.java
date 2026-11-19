package com.mohamed.barcodelabel.workflow.data;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Function;

/**
 * An immutable list view of consecutive repetitions, storing one value and cumulative count per run.
 * Values must themselves be immutable when shared. Mapping snapshots each run once, not each repetition.
 */
public final class RepeatedList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> values;
    private final int[] ends;

    private RepeatedList(List<T> values, int[] ends) {
        this.values = List.copyOf(values);
        this.ends = ends;
    }

    public static <T> RepeatedList<T> of(List<T> values, List<Integer> copies) {
        if (values.size() != copies.size()) throw new IllegalArgumentException("Each value needs a copy count");
        int[] ends = new int[copies.size()];
        int total = 0;
        for (int i = 0; i < ends.length; i++) {
            int count = copies.get(i);
            if (count <= 0) throw new IllegalArgumentException("Copy counts must be positive");
            total = Math.addExact(total, count);
            ends[i] = total;
        }
        return new RepeatedList<>(values, ends);
    }

    /** Values in source order, once per run (even if two runs happen to have equal values). */
    public List<T> values() { return values; }

    /** Exclusive expanded index at the end of this run. */
    public int runEnd(int run) { return ends[run]; }

    public boolean hasSameRuns(RepeatedList<?> other) { return Arrays.equals(ends, other.ends); }

    public <R> RepeatedList<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = values.stream().<R>map(mapper).toList();
        return new RepeatedList<>(mapped, ends);
    }

    /** Snapshot a list without expanding it if it is already represented as repetitions. */
    public static <T, R> List<R> mapCopy(List<T> input, Function<? super T, ? extends R> mapper) {
        if (input instanceof RepeatedList<T> repeated) return repeated.map(mapper);
        return input.stream().<R>map(mapper).toList();
    }

    /** Preserve a known immutable repeated view; defensively copy all other lists. */
    public static <T> List<T> copyOf(List<T> input) {
        return input instanceof RepeatedList<T> ? input : List.copyOf(input);
    }

    @Override public T get(int index) {
        Objects.checkIndex(index, size());
        int run = Arrays.binarySearch(ends, index + 1);
        return values.get(run >= 0 ? run : -run - 1);
    }

    @Override public int size() { return ends.length == 0 ? 0 : ends[ends.length - 1]; }

    // Clearing even an empty view is forbidden; sublists must not expose a mutation path.
    @Override public List<T> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(super.subList(fromIndex, toIndex));
    }
    @Override public void clear() { throw new UnsupportedOperationException(); }
}
