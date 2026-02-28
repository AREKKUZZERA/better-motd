package bettermotd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

final class StickyStateSupport {

    private StickyStateSupport() {
    }

    static <K, V> void cleanupExpired(Map<K, V> entries, long nowMs, long ttlMs, int batchLimit,
            BiPredicate<V, Long> isValid) {
        int checked = 0;
        for (Map.Entry<K, V> entry : entries.entrySet()) {
            if (checked >= batchLimit) {
                break;
            }
            checked++;
            if (!isValid.test(entry.getValue(), nowMs - ttlMs)) {
                entries.remove(entry.getKey(), entry.getValue());
            }
        }
    }

    static <K, V> void enforceLimit(Map<K, V> entries, Deque<K> order, int maxEntries, int evictionBatch) {
        if (maxEntries <= 0 || entries.size() <= maxEntries) {
            return;
        }

        int evicted = 0;
        while (entries.size() > maxEntries && evicted < evictionBatch) {
            K key = order.pollFirst();
            if (key == null) {
                break;
            }
            if (entries.remove(key) != null) {
                evicted++;
            }
        }

        if (entries.size() > maxEntries) {
            List<K> candidates = new ArrayList<>(evictionBatch);
            for (K key : entries.keySet()) {
                candidates.add(key);
                if (candidates.size() >= evictionBatch) {
                    break;
                }
            }
            candidates.sort((left, right) -> String.valueOf(left).compareTo(String.valueOf(right)));
            for (K key : candidates) {
                if (entries.size() <= maxEntries) {
                    break;
                }
                entries.remove(key);
            }
        }
    }
}
