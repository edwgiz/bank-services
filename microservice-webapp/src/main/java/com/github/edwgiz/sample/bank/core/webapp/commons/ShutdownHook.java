package com.github.edwgiz.sample.bank.core.webapp.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Performs actions in FIFO manner at application exit time.
 *
 * @see Runtime#addShutdownHook(java.lang.Thread)
 */
@Singleton
public final class ShutdownHook {

    private static final Logger LOG = LoggerFactory.getLogger(ShutdownHook.class);

    private final ConcurrentLinkedDeque<AutoCloseable> shutdownActions;
    private final Thread thread;

    /**
     * @param name name will be used for a shutdown thread and for logging
     */
    public ShutdownHook(final String name) {
        shutdownActions = new ConcurrentLinkedDeque<>();
        thread = new Thread(() -> {
            for (;;) {
                final AutoCloseable action = shutdownActions.poll();
                if (action == null) {
                    break;
                }
                try {
                    action.close();
                } catch (Throwable ex) {
                    LOG.error(name, ex);
                }
            }
        }, name);
        Runtime.getRuntime().addShutdownHook(thread);
    }

    /* default */Collection<AutoCloseable> getShutdownActions() {
        return shutdownActions;
    }

    /* default */Thread getThread() {
        return thread;
    }

    /**
     * @param shutdownActon auto-closeable to delayed FIFO execution at application exit time.
     */
    public void addLast(final AutoCloseable shutdownActon) {
        shutdownActions.addLast(shutdownActon);
    }

    /**
     * @param shutdownActon auto-closeable to delayed FILO execution at application exit time.
     */
    public void addFirst(final AutoCloseable shutdownActon) {
        shutdownActions.addFirst(shutdownActon);
    }
}
