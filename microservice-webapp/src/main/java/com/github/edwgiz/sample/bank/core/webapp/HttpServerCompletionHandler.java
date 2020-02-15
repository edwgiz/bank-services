package com.github.edwgiz.sample.bank.core.webapp;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class HttpServerCompletionHandler implements CompletionHandler<HttpServer> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerCompletionHandler.class);

    /**
     * Message part to be used from the unit test.
     */
    /* default */static final String MSG_PREFIX = "Shutdown HTTP server";

    private final long timeoutMillis;
    private final CountDownLatch countDownLatch;

    @SuppressWarnings("checkstyle:HiddenField")
    /* default */HttpServerCompletionHandler(final long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void cancelled() {
        LOG.warn(MSG_PREFIX + " - cancelled");
        countDownLatch.countDown();
    }

    @Override
    public void failed(final Throwable throwable) {
        LOG.error(MSG_PREFIX + " - failed", throwable);
        countDownLatch.countDown();
    }

    @Override
    public void completed(final HttpServer result) {
        LOG.debug(MSG_PREFIX + " - on-complete");
        countDownLatch.countDown();
    }

    @Override
    public void updated(final HttpServer result) {
        LOG.warn(MSG_PREFIX + " - updated");
    }

    /* default */boolean await() {
        boolean result = false;
        try {
            result = countDownLatch.await(timeoutMillis, MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn(MSG_PREFIX + " - interrupted", e);
        }
        return result;
    }
}
