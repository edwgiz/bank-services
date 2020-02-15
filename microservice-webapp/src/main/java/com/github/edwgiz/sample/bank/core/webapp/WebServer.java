package com.github.edwgiz.sample.bank.core.webapp;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class WebServer {

    private static final Logger LOG = LoggerFactory.getLogger(WebServer.class);

    /**
     * Required property to be passed via command-line.
     */
    static final /* default */ String SYSTEM_PROPERTY_HOSTNAME = "webserver.http.hostname";
    /**
     * Required property to be passed via command-line.
     */
    static final /* default */ String SYSTEM_PROPERTY_PORT = "webserver.http.port";

    public AutoCloseable start(final ResourceConfig conf) {
        final HttpServer httpServer = createHttpServer(conf);
        final ServerConfiguration httpServerConf = httpServer.getServerConfiguration();
        httpServerConf.setSessionManager(null);
        httpServerConf.setGracefulShutdownSupported(true);
        attachStaticContent(httpServer);
        final AutoCloseable shutdownAction = createShutdownAction(httpServer, 60_000L);
        try {
            httpServer.start();
        } catch (IOException e) {
            LOG.error("Start server - failed", e);
        }
        return shutdownAction;
    }

    /* default */void attachStaticContent(final HttpServer httpServer) {
        final String staticContentPath = "/swagger-ui";
        final CLStaticHttpHandler httpHandler = new CLStaticHttpHandler(getClass().getClassLoader(),
                staticContentPath + '/');
        httpHandler.setFileCacheEnabled(false);
        httpServer.getServerConfiguration().addHttpHandler(httpHandler, staticContentPath);
    }

    /* default */HttpServer createHttpServer(final ResourceConfig conf) {
        final String host = getSystemProperty(SYSTEM_PROPERTY_HOSTNAME);
        final String port = getSystemProperty(SYSTEM_PROPERTY_PORT);
        final URI uri;
        try {
            uri = new URI("http", null, host, Integer.parseInt(port), null, null, null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Can't create URI by given host and port '" + host + "', '" + port + '\'', e);
        }
        return GrizzlyHttpServerFactory.createHttpServer(uri, conf, false, null, false);
    }

    static /* default */String getSystemProperty(final String key) {
        final String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Can't find '" + key + "' in system properties");
        }
        return value;
    }

    /* default */AutoCloseable createShutdownAction(final HttpServer httpServer, final long timeoutMillis) {
        return () -> {
            LOG.info(HttpServerCompletionHandler.MSG_PREFIX + " initiated...");
            final HttpServerCompletionHandler completionHandler = new HttpServerCompletionHandler(timeoutMillis);
            httpServer.shutdown(timeoutMillis, MILLISECONDS).addCompletionHandler(completionHandler);
            if (completionHandler.await()) {
                LOG.info("Shutdown HTTP server - completed");
            } else {
                LOG.warn("Shutdown HTTP server - forced exit");
            }
        };
    }
}
