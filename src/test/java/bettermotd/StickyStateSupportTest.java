package bettermotd;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StickyStateSupportTest {

    @Test
    void cleanupExpiredRemovesStaleEntriesWithoutIteratorRemoval() {
        Map<String, Long> entries = new ConcurrentHashMap<>();
        entries.put("fresh", 1000L);
        entries.put("stale", 100L);

        StickyStateSupport.cleanupExpired(entries, 2000L, 1000L, 100,
                (createdAt, threshold) -> createdAt >= threshold);

        assertTrue(entries.containsKey("fresh"));
        assertFalse(entries.containsKey("stale"));
    }

    @Test
    void concurrentCleanupAndEvictionRemainStable() throws Exception {
        Map<String, Long> entries = new ConcurrentHashMap<>();
        ConcurrentLinkedDeque<String> order = new ConcurrentLinkedDeque<>();

        for (int i = 0; i < 500; i++) {
            String key = "ip-" + i;
            entries.put(key, (long) i);
            order.addLast(key);
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 50; i++) {
            int idx = i;
            pool.submit(() -> {
                String key = "new-" + idx;
                entries.put(key, 10_000L);
                order.addLast(key);
                StickyStateSupport.cleanupExpired(entries, 10_000L, 1_000L, 200,
                        (createdAt, threshold) -> createdAt >= threshold);
                StickyStateSupport.enforceLimit(entries, order, 200, 200);
            });
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(entries.size() <= 200);
    }
}
