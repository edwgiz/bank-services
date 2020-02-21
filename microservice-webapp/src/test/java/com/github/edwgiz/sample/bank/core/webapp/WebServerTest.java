package com.github.edwgiz.sample.bank.core.webapp;

import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;

import static com.github.edwgiz.sample.bank.core.webapp.WebServer.SYSTEM_PROPERTY_HOSTNAME;
import static com.github.edwgiz.sample.bank.core.webapp.WebServer.SYSTEM_PROPERTY_PORT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.glassfish.grizzly.http.server.HttpHandlerRegistration.builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class WebServerTest {

    private static final long SHUTDOWN_TIMEOUT_MILLIS = 60_000L;


    @Test
    public void testStart() throws IOException {
        final WebServer webServer = mock(WebServer.class);
        doCallRealMethod().when(webServer).start(any());
        final HttpServer httpServer = mock(HttpServer.class);
        final ResourceConfig conf = mock(ResourceConfig.class);
        doReturn(httpServer).when(webServer).createHttpServer(eq(conf));
        final ServerConfiguration httpServerConf = mock(ServerConfiguration.class);
        //noinspection ResultOfMethodCallIgnored
        doReturn(httpServerConf).when(httpServer).getServerConfiguration();
        final AutoCloseable shutdownAction = mock(AutoCloseable.class);
        //noinspection ResultOfMethodCallIgnored
        doReturn(shutdownAction).when(webServer).createShutdownAction(eq(httpServer), eq(SHUTDOWN_TIMEOUT_MILLIS));

        testNormalStart(webServer, httpServer, conf, httpServerConf, shutdownAction);

        doThrow(IOException.class).when(httpServer).start();
        testNormalStart(webServer, httpServer, conf, httpServerConf, shutdownAction); // IOException affects only log
    }

    private void testNormalStart(final WebServer webServer, final HttpServer httpServer, final ResourceConfig conf,
            final ServerConfiguration httpServerConf, final AutoCloseable shutdownAction) throws IOException {
        final InOrder inOrder = inOrder(webServer, httpServer, httpServerConf);
        assertSame(shutdownAction, webServer.start(conf));
        inOrder.verify(webServer, calls(1)).createHttpServer(eq(conf));
        //noinspection ResultOfMethodCallIgnored
        inOrder.verify(httpServer, calls(1)).getServerConfiguration();
        inOrder.verify(httpServerConf, calls(1)).setSessionManager(isNull());
        inOrder.verify(httpServerConf, calls(1)).setGracefulShutdownSupported(eq(true));
        inOrder.verify(webServer, calls(1)).attachStaticContent(same(httpServer));
        //noinspection ResultOfMethodCallIgnored
        inOrder.verify(webServer, calls(1))
                .createShutdownAction(same(httpServer), eq(SHUTDOWN_TIMEOUT_MILLIS));
        inOrder.verify(httpServer, calls(1)).start();
    }

    @Test
    public void testAttachStaticContent() {
        final HttpServer httpServer = mock(HttpServer.class);
        final ServerConfiguration serverConfiguration = mock(ServerConfiguration.class);
        //noinspection ResultOfMethodCallIgnored
        doReturn(serverConfiguration).when(httpServer).getServerConfiguration();

        final WebServer webServer = new WebServer();
        webServer.attachStaticContent(httpServer);
        Mockito.verify(serverConfiguration, times(1)).addHttpHandler(
                Mockito.argThat((httpHandler) -> {
                    Assertions.assertTrue(httpHandler instanceof CLStaticHttpHandler);
                    final CLStaticHttpHandler sch = (CLStaticHttpHandler) httpHandler;
                    assertFalse(sch.isFileCacheEnabled());
                    assertEquals(WebServer.class.getClassLoader(), sch.getClassLoader());
                    return true;
                }), eq(builder().urlPattern("/swagger-ui/*").build()),
                eq(builder().urlPattern("/openapi.json").build()));
    }

    @Test
    public void testCreateHttpServer() {
        final WebServer webServer = new WebServer();
        final ResourceConfig conf = new ResourceConfig();
        System.setProperty(SYSTEM_PROPERTY_HOSTNAME, "127.0.0.1");
        System.setProperty(SYSTEM_PROPERTY_PORT, "X");
        assertThrows(IllegalStateException.class, () -> webServer.createHttpServer(conf));

        System.setProperty(SYSTEM_PROPERTY_HOSTNAME, "\\");
        System.setProperty(SYSTEM_PROPERTY_PORT, "8080");
        assertThrows(IllegalStateException.class, () -> webServer.createHttpServer(conf));

        System.setProperty(SYSTEM_PROPERTY_HOSTNAME, "127.0.0.1");
        System.setProperty(SYSTEM_PROPERTY_PORT, "8080");
        final HttpServer httpServer = webServer.createHttpServer(conf);
        assertFalse(httpServer.isStarted());
    }

    @Test
    public void testGetSystemProperty() {
        Assertions.assertNotNull(WebServer.getSystemProperty("java.version"));
        assertThrows(IllegalArgumentException.class,
                () -> WebServer.getSystemProperty("unknown"),
                "Can't find 'unknown' in system properties");
    }

    @Test
    public void testCreateShutdownAction() throws Exception {
        @SuppressWarnings("unchecked") final GrizzlyFuture<HttpServer> serverShutdownFuture = mock(GrizzlyFuture.class);
        final long timeoutMillis = 500L;
        final HttpServer server = mock(HttpServer.class);
        doReturn(serverShutdownFuture).when(server).shutdown(timeoutMillis, MILLISECONDS);
        final AutoCloseable shutdownAction = new WebServer().createShutdownAction(server, timeoutMillis);

        // test the shutdown action
        shutdownAction.close(); // wait until the timeout
        Mockito.verify(serverShutdownFuture).addCompletionHandler(Mockito.notNull());

        Mockito.doNothing().when(serverShutdownFuture).addCompletionHandler(Mockito.argThat((completionHandler) -> {
            completionHandler.completed(server); // send completion event before timeout
            return true;
        }));
        shutdownAction.close();
    }
}
