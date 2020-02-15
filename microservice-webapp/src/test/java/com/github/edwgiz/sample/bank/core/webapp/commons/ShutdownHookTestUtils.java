package com.github.edwgiz.sample.bank.core.webapp.commons;

/**
 * Provides access to {@link ShutdownHook} fields with a package-level accessibility.
 */
public final class ShutdownHookTestUtils {

    /**
     * @param shutdownHook an instance to access.
     * @return underlying shutdown thread.
     */
    public static Thread getThread(final ShutdownHook shutdownHook) {
        return shutdownHook.getThread();
    }


    private ShutdownHookTestUtils() {
    }
}
