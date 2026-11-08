package de.raindancer.modules.chat.store;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Turns "write now" calls from the server thread into one write a moment later, off that thread: a restart
 * that kicks a hundred players asks a hundred times and the database sees one write.
 *
 * <p>{@link #close()} writes whatever is still waiting on the calling thread, so nothing asked for is lost on
 * shutdown; after it, requests write immediately.
 */
public final class DebouncedFlush implements AutoCloseable {

    private final ScheduledExecutorService pool;
    private final long delayMillis;
    private final BooleanSupplier write;
    private boolean scheduled;
    private boolean closed;

    /** @param write the actual write; returns false when it failed, which leaves nothing scheduled — the next request retries */
    public DebouncedFlush(ScheduledExecutorService pool, long delayMillis, BooleanSupplier write) {
        this.pool = pool;
        this.delayMillis = delayMillis;
        this.write = write;
    }

    public void request() {
        synchronized (this) {
            if (!closed) {
                if (!scheduled) {
                    scheduled = true;
                    pool.schedule(this::run, delayMillis, TimeUnit.MILLISECONDS);
                }
                return;
            }
        }
        write.getAsBoolean();
    }

    private void run() {
        synchronized (this) {
            if (!scheduled) {
                return;
            }
            scheduled = false;
        }
        write.getAsBoolean();
    }

    @Override
    public void close() {
        boolean pending;
        synchronized (this) {
            closed = true;
            pending = scheduled;
            scheduled = false;
        }
        if (pending) {
            write.getAsBoolean();
        }
    }
}
