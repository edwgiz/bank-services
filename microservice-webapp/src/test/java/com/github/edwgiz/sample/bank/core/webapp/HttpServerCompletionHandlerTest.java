package com.github.edwgiz.sample.bank.core.webapp;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class HttpServerCompletionHandlerTest {

    private static ExecutorService executor;
    private static final long TIMEOUT_MILLIS = 500L;

    @BeforeAll
    static /* default */void beforeAll() {
        executor = Executors.newSingleThreadExecutor();
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testInterrupt() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        final CompletableFuture<Thread> capturedShutdownThread = new CompletableFuture<>();
        final Future<Boolean> serverCompletionAwaitResult = executor.submit(() -> {
            capturedShutdownThread.complete(Thread.currentThread());
            return completionHandler.await();
        });
        final Thread shutdownThread = capturedShutdownThread.get();
        for (int i = 0; i < 100; i++) { //wait for 5 sec
            System.out.println(shutdownThread.getState());
            switch (shutdownThread.getState()) {
                case WAITING:
                case TIMED_WAITING:
                    assertFalse(shutdownThread.isInterrupted());
                    shutdownThread.interrupt();
                    assertFalse(serverCompletionAwaitResult.get());
                    return;
                default:
            }
            Thread.sleep(50);
        }
        fail("Test timeout exceeded");
    }

    private HttpServerCompletionHandler create() {
        return new HttpServerCompletionHandler(TIMEOUT_MILLIS);
    }

    @Test
    public void testTimeoutExceeded() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        testAwait(completionHandler, () -> {
        }, false);
    }

    @Test
    public void testOnCancelled() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        testAwait(completionHandler, completionHandler::cancelled, true);
    }

    @Test
    public void testOnFailed() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        testAwait(completionHandler, () ->
                completionHandler.failed(new Throwable("on failed test throwable instance")), true);
    }

    @Test
    public void testOnCompleted() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        final HttpServer httpServer = Mockito.mock(HttpServer.class);
        testAwait(completionHandler, () -> completionHandler.completed(httpServer), true);
        Mockito.verifyNoInteractions(httpServer);
    }

    @Test
    public void testOnUpdated() throws Exception {
        final HttpServerCompletionHandler completionHandler = create();
        final HttpServer httpServer = Mockito.mock(HttpServer.class);
        testAwait(completionHandler, () -> completionHandler.updated(httpServer), false);
        Mockito.verifyNoInteractions(httpServer);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private void testAwait(final HttpServerCompletionHandler completionHandler, final Runnable action,
            final boolean expected) throws Exception {
        final Future<Boolean> awaitFuture = executor.submit(completionHandler::await);
        // wait 80% of timeout
        Thread.sleep(TIMEOUT_MILLIS * 8 / 10);
        assertFalse(awaitFuture.isDone());
        action.run();
        // wait +40% of timeout, 120% in sum
        assertEquals(expected, awaitFuture.get(TIMEOUT_MILLIS * 4 / 10, TimeUnit.MILLISECONDS));
    }

    @AfterAll
    static /* default */void afterAll() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
