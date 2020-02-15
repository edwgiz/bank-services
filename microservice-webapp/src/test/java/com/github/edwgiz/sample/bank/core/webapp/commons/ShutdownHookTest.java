package com.github.edwgiz.sample.bank.core.webapp.commons;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.stream.IntStream;

import static org.apache.commons.lang3.ArrayUtils.add;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class ShutdownHookTest {

    @Test
    public void test() throws Exception {
        final ShutdownHook shutdownHook = new ShutdownHook("test hook");
        test(shutdownHook);
    }

    /**
     * Tests that all actions were invoked in FIFO and despite the exceptions.
     *
     * @param shutdownHook shutdown hook.
     * @throws Exception thrown by eny error.
     */
    public static void test(final ShutdownHook shutdownHook) throws Exception {
        //
        @SuppressWarnings("checkstyle:MagicNumber") final int endExclusive = 100;
        final AutoCloseable[] actions = IntStream.range(1, endExclusive)
                .mapToObj(i -> mock(AutoCloseable.class, "a" + i))
                .toArray(AutoCloseable[]::new);

        for (int i = 0; i < actions.length; i++) {
            final AutoCloseable action = actions[i];
            @SuppressWarnings("checkstyle:MagicNumber") final int throwExceptionEach = 7;
            @SuppressWarnings("checkstyle:MagicNumber") final int throwErrorEach = 31;
            if (i % throwExceptionEach == 0) {
                doThrow(Exception.class).when(action).close();
            } else if (i % throwErrorEach == 0) {
                doThrow(Error.class).when(action).close();
            }

            shutdownHook.addLast(action);
        }
        final AutoCloseable foremost = mock(AutoCloseable.class, "first");
        shutdownHook.addFirst(foremost);

        @SuppressWarnings("RedundantCast") final InOrder order = Mockito.inOrder((Object[]) add(actions, foremost));

        await(shutdownHook);

        order.verify(foremost).close();
        for (final AutoCloseable a : actions) {
            order.verify(a).close();
        }
        order.verifyNoMoreInteractions();
    }

    /**
     * Unregister the given shutdown hook from a system,
     * starts it in method,
     * waits for completion and
     * checks its action queue is empty.
     *
     * @param shutdownHook shutdown hook to execute and
     * @throws InterruptedException thrown if the shutdown thread is received interrupt signal
     */
    public static void await(final ShutdownHook shutdownHook) throws InterruptedException {
        final Thread shutdownThread = ShutdownHookTestUtils.getThread(shutdownHook);

        // ShutdownHook#thread must been added to the system shutdown hooks
        assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownThread));

        shutdownThread.start();
        shutdownThread.join();

        assertShutdownActionsSize(0, shutdownHook);
    }

    /**
     * @param expectedSize expected size of shutdown action queue in given {@code shutdownHook}.
     * @param shutdownHook shutdown hook to assert.
     */
    public static void assertShutdownActionsSize(final int expectedSize, final ShutdownHook shutdownHook) {
        Assertions.assertEquals(expectedSize, shutdownHook.getShutdownActions().size());
    }
}
