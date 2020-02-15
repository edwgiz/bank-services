package com.github.edwgiz.sample.bank.core.webapp;

import com.github.edwgiz.sample.bank.core.webapp.commons.ShutdownHook;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.function.Supplier;

import static com.github.edwgiz.sample.bank.core.webapp.commons.ShutdownHookTestUtils.getThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WebAppBaseTest {

    @Test
    public void testNormalStart() {
        final WebAppBase webAppBase = mock(WebAppBase.class);
        final ShutdownHook shutdownHook = mock(ShutdownHook.class);
        final ResourceConfig resourceConfig = mock(ResourceConfig.class);
        final WebServer webServer = mock(WebServer.class);
        final AutoCloseable shutdownAction = mock(AutoCloseable.class);

        doCallRealMethod().when(webAppBase).start();
        doReturn(shutdownHook).when(webAppBase).instantiateShutdownHook(notNull());
        doReturn(resourceConfig).when(webAppBase).createResourceConfig();
        //noinspection ResultOfMethodCallIgnored
        doReturn(webServer).when(webAppBase).createWebServer();
        doReturn(shutdownAction).when(webServer).start(same(resourceConfig));

        final InOrder inOrder = inOrder(webAppBase, shutdownHook, resourceConfig, webServer);
        webAppBase.start();
        inOrder.verify(webAppBase, calls(1)).initLogging();
        inOrder.verify(webAppBase, calls(1)).instantiateShutdownHook(notNull());
        inOrder.verify(webAppBase, calls(1)).createResourceConfig();
        //noinspection ResultOfMethodCallIgnored
        inOrder.verify(webAppBase, calls(1)).createWebServer();
        inOrder.verify(webServer, calls(1)).start(same(resourceConfig));
        inOrder.verify(shutdownHook, calls(1)).addLast(same(shutdownAction));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testAbnormalStart() {
        final WebAppBase webAppBase = mock(WebAppBase.class);
        final Runtime runtime = mock(Runtime.class);

        doCallRealMethod().when(webAppBase).start();
        Mockito.doThrow(ExceptionInInitializerError.class).when(webAppBase).instantiateShutdownHook(notNull());
        //noinspection ResultOfMethodCallIgnored
        doReturn(runtime).when(webAppBase).getRuntime();
        final InOrder inOrder = inOrder(webAppBase, runtime);
        webAppBase.start();
        inOrder.verify(webAppBase, calls(1)).initLogging();
        inOrder.verify(webAppBase, calls(1)).instantiateShutdownHook(notNull());
        //noinspection ResultOfMethodCallIgnored
        inOrder.verify(webAppBase, calls(1)).getRuntime();
        inOrder.verify(runtime, calls(1)).exit(eq(1));
    }

    @Test
    public void testInstantiateShutdownHook() {
        final ShutdownHook shutdownHook = new WebAppBase().instantiateShutdownHook("test name");
        assertTrue(Runtime.getRuntime().removeShutdownHook(getThread(shutdownHook)));
    }

    @Test
    public void testInitLogging() {
        assertFalse(System.getProperties().containsKey("org.jboss.logging.provider"));
        if (SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
        }
        assertFalse(SLF4JBridgeHandler.isInstalled());

        new WebAppBase().initLogging();

        assertTrue(System.getProperties().containsKey("org.jboss.logging.provider"));
        assertTrue(SLF4JBridgeHandler.isInstalled());
    }

    @Test
    public void testCreateWebServer() {
        assertNotNull(new WebAppBase().createWebServer());
    }

    @Test
    public void testGetRuntime() {
        Assertions.assertSame(Runtime.getRuntime(), new WebAppBase().getRuntime());
    }

    @Test
    public void testCreateResourceConfig() {
        final WebAppBase webAppBase = mock(WebAppBase.class);
        @SuppressWarnings("unchecked") final Supplier<ResourceConfig> jaxRsApplicationFactory = mock(Supplier.class);
        final ResourceConfig resourceConfig = mock(ResourceConfig.class);

        doCallRealMethod().when(webAppBase).createResourceConfig();
        doReturn(jaxRsApplicationFactory).when(webAppBase).getJaxRsApplicationFactory();
        doReturn(resourceConfig).when(jaxRsApplicationFactory).get();
        Assertions.assertEquals(resourceConfig, webAppBase.createResourceConfig());
        verify(webAppBase, times(1)).createResourceConfig();
        verify(webAppBase, times(1)).getJaxRsApplicationFactory();
        verify(jaxRsApplicationFactory, times(1)).get();
    }

    @Test
    public void testGetJaxRsApplicationFactory() {
        assertNotNull(new WebAppBase().getJaxRsApplicationFactory());
    }
}
