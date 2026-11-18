package com.mohamed.barcodelabel.workflow.data;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RepeatedListTest {
    @Test void behavesLikeAnExpandedImmutableListAcrossRunBoundaries() {
        var values = new ArrayList<>(List.of("A", "B", "C"));
        var counts = new ArrayList<>(List.of(2, 1, 3));
        var list = RepeatedList.of(values, counts);
        values.set(0, "changed"); counts.set(0, 100);
        var expanded = List.of("A", "A", "B", "C", "C", "C");
        assertEquals(expanded, list);
        assertEquals(list, expanded);
        assertEquals(expanded.hashCode(), list.hashCode());
        assertArrayEquals(expanded.toArray(), list.toArray());
        assertEquals(expanded, list.stream().toList());
        assertEquals(List.of("A", "B", "C"), list.subList(1, 4));
        var iterator = list.listIterator(3);
        assertEquals("B", iterator.previous());
        assertEquals("B", iterator.next());
        assertEquals("C", iterator.next());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertThrows(UnsupportedOperationException.class, () -> list.set(0, "X"));
        assertThrows(UnsupportedOperationException.class, () -> list.add("X"));
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> list.subList(0, 1).clear());
        assertThrows(UnsupportedOperationException.class, () -> list.values().clear());
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(6));
    }

    @Test void snapshotsAndMapsEachLogicalValueOnceEvenForMillionsOfCopies() {
        var list = RepeatedList.of(List.of("A", "B"), List.of(2_000_000, 3_000_000));
        var calls = new AtomicInteger();
        var mapped = RepeatedList.mapCopy(list, value -> { calls.incrementAndGet(); return value.toLowerCase(); });
        assertEquals(2, calls.get());
        assertEquals(5_000_000, mapped.size());
        assertEquals("a", mapped.get(1_999_999));
        assertEquals("b", mapped.get(2_000_000));
        assertEquals("b", mapped.getLast());
        assertSame(mapped, RepeatedList.copyOf(mapped));
        var ordinary = new ArrayList<>(List.of("A", "B"));
        var snapshot = RepeatedList.copyOf(ordinary);
        var transformed = RepeatedList.mapCopy(ordinary, String::toLowerCase);
        ordinary.clear();
        assertEquals(List.of("A", "B"), snapshot);
        assertEquals(List.of("a", "b"), transformed);
    }

    @Test void validatesCountsAndHandlesEmptyListsWithoutAllocatingAnExpandedArray() {
        var empty = RepeatedList.of(List.of(), List.of());
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, empty::clear);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IllegalArgumentException.class, () -> RepeatedList.of(List.of("A"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> RepeatedList.of(List.of("A"), List.of(0)));
        assertThrows(IllegalArgumentException.class, () -> RepeatedList.of(List.of("A"), List.of(-1)));
        assertThrows(ArithmeticException.class, () -> RepeatedList.of(List.of("A", "B"), List.of(Integer.MAX_VALUE, 1)));
    }
}
