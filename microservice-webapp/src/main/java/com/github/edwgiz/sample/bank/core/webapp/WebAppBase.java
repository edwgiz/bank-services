package com.github.edwgiz.sample.bank.core.webapp;

import com.github.edwgiz.sample.bank.core.commons.ExcludeFromJacocoMetrics;
import com.github.edwgiz.sample.bank.core.webapp.commons.ShutdownHook;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.function.Supplier;

import static java.lang.System.nanoTime;


/**
 * Application bootstrap.
 *
 * <ol>
 * <li>Prevents unpredictable logging initialization in third-party libraries.</li>
 * <li>Creates Jersey's resource config.</li>
 * <li>Creates HK2 binder that in its base version tears JDBC up.</li>
 * <li>Starts embedded HTTP server.</li>
 * </ol>
 * <p>
 * System properties required to define {@code webserver.http.hostname} and {@code webserver.http.port}
 * <br/>
 * Example of its VM options:
 * <pre>-Dwebserver.http.hostname=127.0.0.1 -Dwebserver.http.port=8080</pre>
 */
public class WebAppBase {

    /**
     * Instantiates an instance of itself and starts it.
     *
     * @param args ignored arguments
     */
    @ExcludeFromJacocoMetrics.Generated()
    public static void main(final String[] args) {
        new WebAppBase().start();
    }

    /**
     * Starts the web server application in another thread.
     */
    public final void start() {
        final long start = nanoTime();
        initLogging();

        final Logger log = LoggerFactory.getLogger(WebAppBase.class);
        try {
            log.info("Start server...");
            final ShutdownHook shutdownHook = instantiateShutdownHook("graceful shutdown");
            final ResourceConfig resourceConfig = createResourceConfig();
            shutdownHook.addLast(createWebServer().start(resourceConfig));

            @SuppressWarnings({"checkstyle:MagicNumber", "IntegerDivisionInFloatingPointContext"})
            final double startupSeconds = ((nanoTime() - start) / 10_000_000) / 1e2;
            log.info("Start server - done in " + startupSeconds + " secs");
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            getRuntime().exit(1);
        }
    }

    final /* default */ShutdownHook instantiateShutdownHook(final String name) {
        return new ShutdownHook(name);
    }

    /**
     * Initializes loggers before any other operations.
     */
    protected void initLogging() {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    final /* default */WebServer createWebServer() {
        return new WebServer();
    }

    final /* default */Runtime getRuntime() {
        return Runtime.getRuntime();
    }

    /**
     * @return config of jax-rs application to be extended in descendants,
     */
    protected ResourceConfig createResourceConfig() {
        return getJaxRsApplicationFactory().get();
    }

    @SuppressWarnings("checkstyle:DesignForExtension") // for unit testing
    /* default */Supplier<ResourceConfig> getJaxRsApplicationFactory() {
        return new JaxRsApplicationFactory();
    }
}
