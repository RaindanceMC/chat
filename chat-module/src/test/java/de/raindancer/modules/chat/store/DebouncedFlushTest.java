package de.raindancer.modules.chat.store;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DebouncedFlushTest {

    private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stop() {
        pool.shutdownNow();
    }

    @Test
    @DisplayName("a burst of requests is one write, off the calling thread")
    void burstIsOneWrite() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        Thread[] writer = new Thread[1];
        CountDownLatch done = new CountDownLatch(1);
        DebouncedFlush flush = new DebouncedFlush(pool, 50, () -> {
            writes.incrementAndGet();
            writer[0] = Thread.currentThread();
            done.countDown();
            return true;
        });

        for (int i = 0; i < 100; i++) {
            flush.request();
        }
        assertThat(writes.get()).isZero();
        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(150);

        assertThat(writes.get()).isEqualTo(1);
        assertThat(writer[0]).isNotSameAs(Thread.currentThread());
    }

    @Test
    @DisplayName("close writes what is still waiting, on the calling thread, and later requests write at once")
    void closeWritesPending() {
        AtomicInteger writes = new AtomicInteger();
        DebouncedFlush flush = new DebouncedFlush(pool, 60_000, () -> {
            writes.incrementAndGet();
            return true;
        });

        flush.request();
        flush.close();
        assertThat(writes.get()).isEqualTo(1);

        flush.request();
        assertThat(writes.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("close with nothing waiting writes nothing")
    void closeIdle() {
        AtomicInteger writes = new AtomicInteger();
        new DebouncedFlush(pool, 10, () -> {
            writes.incrementAndGet();
            return true;
        }).close();
        assertThat(writes.get()).isZero();
    }

    @Test
    @DisplayName("a write that fails is tried again on the next request")
    void failedWriteIsRetried() throws Exception {
        AtomicInteger writes = new AtomicInteger();
        CountDownLatch twice = new CountDownLatch(2);
        DebouncedFlush flush = new DebouncedFlush(pool, 20, () -> {
            twice.countDown();
            return writes.incrementAndGet() > 1;
        });
        flush.request();
        Thread.sleep(150);
        flush.request();
        assertThat(twice.await(2, TimeUnit.SECONDS)).isTrue();
    }
}
