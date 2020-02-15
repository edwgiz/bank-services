package com.github.edwgiz.sample.bank.core.storage;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.h2.jdbc.JdbcSQLTimeoutException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Also tests that data source supports the required features.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InMemoryDataSourceFactoryTest {

    private static DataSource dataSource;
    private static InMemoryDataSourceFactory dataSourceFactory;

    @Test
    @Order(1)
        /* default */void testCreateFlywayConfiguration() {
        final FluentConfiguration expected = Flyway.configure();
        final FluentConfiguration actual = new InMemoryDataSourceFactory().createFlywayConfiguration();
        assertTrue(new EqualsBuilder().setTestRecursive(true).setTestTransients(false)
                .reflectionAppend(expected, actual)
                .isEquals());
    }

    @Test
    @Order(2)
        /* default */void testProvide() {
        dataSource = testAndGetDataSource();
    }

    /* default */DataSource testAndGetDataSource() {
        dataSourceFactory = new InMemoryDataSourceFactory();
        final InMemoryDataSourceFactory dsfMock = spy(dataSourceFactory);
        final Flyway flyway = mock(Flyway.class);
        final FluentConfiguration fwCnf = mock(FluentConfiguration.class);
        final ArgumentCaptor<DataSource> dataSourceArgumentCaptor = ArgumentCaptor.forClass(DataSource.class);
        doReturn(fwCnf).when(fwCnf).dataSource(dataSourceArgumentCaptor.capture());
        doReturn(flyway).when(fwCnf).load();
        doReturn(fwCnf).when(dsfMock).createFlywayConfiguration();
        final DataSource providedDataSource = dsfMock.provide();
        assertSame(providedDataSource, dataSourceArgumentCaptor.getValue());
        final InOrder inOrder = inOrder(dsfMock, flyway, fwCnf);
        inOrder.verify(flyway, times(1)).migrate();
        inOrder.verifyNoMoreInteractions();
        return providedDataSource;
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")// false positive for @Order
    @Order(3)
    public void testExclusiveLock() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection con = dataSource.getConnection(); Statement st1 = con.createStatement()) {
            assertFalse(st1.execute("CREATE TABLE entity (a IDENTITY, b VARCHAR(100))"));
            assertFalse(st1.execute("INSERT INTO entity(b) VALUES('AAA')"));


            // hold less than default database lock timeout
            final Future<Exception> f2success = acquireTwoLocks(executor, st1,
                    InMemoryDataSourceFactory.EXCLUSIVE_LOCK_MILLIS * 2 / 3);
            assertFalse(f2success.isDone());
            st1.getConnection().setAutoCommit(true); // release the first lock
            f2success.get(InMemoryDataSourceFactory.EXCLUSIVE_LOCK_MILLIS, TimeUnit.MILLISECONDS);
            // no timeout or execution expected here

            // hold more than default database lock timeout
            final Future<Exception> f2failure = acquireTwoLocks(executor, st1,
                    InMemoryDataSourceFactory.EXCLUSIVE_LOCK_MILLIS * 3 / 2);
            Assertions.assertThrows(JdbcSQLTimeoutException.class, () -> {
                try {
                    f2failure.get();
                } catch (ExecutionException ex) {
                    throw ex.getCause(); //unwrap
                }
            });
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Acquires two locks on the same record from different threads.
     *
     * @param executor                     executor service
     * @param st1                    statement to hold the first lock
     * @param firstLockHoldingMillis how long to hold the first lock
     * @return future of the second lock
     * @throws Exception pass-through exception
     */
    private Future<Exception> acquireTwoLocks(final ExecutorService executor, final Statement st1,
            final long firstLockHoldingMillis)
            throws Exception {
        st1.getConnection().setAutoCommit(false);
        assertTrue(st1.executeQuery("SELECT * FROM entity WHERE b='AAA' FOR UPDATE").next()); // first lock

        final Future<Exception> ftr2 = executor.submit(() -> {
            try (Connection con2 = dataSource.getConnection(); Statement st2 = con2.createStatement()) {
                // wait here for the first lock
                assertTrue(st2.executeQuery("SELECT b FROM entity WHERE b='AAA' FOR UPDATE").next());
            }
            return null;
        });
        sleep(firstLockHoldingMillis);
        return ftr2;
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")// false positive for @Order
    @Order(4)
        /* default */void testDisposeWithEatenException() throws SQLException {
        final InMemoryDataSourceFactory dsfMock = spy(dataSourceFactory);
        final DataSource dsMock = spy(dataSource);
        doThrow(new SQLException("exception to ignore")).when(dsMock).getConnection();
        dsfMock.dispose(dsMock);
        verify(dsfMock, times(1)).dispose(dsMock);
        verify(dsMock, times(1)).getConnection();
        verifyNoMoreInteractions(dsfMock, dsMock);
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")// false positive for @Order
    @Order(5)
        /* default */void testDispose() throws SQLException {
        try (Connection con = dataSource.getConnection()) {
            con.createStatement().executeUpdate("CREATE TABLE shutdown_sensitive (a IDENTITY)");
        }
        dataSourceFactory.dispose(dataSource);
        try (Connection con = dataSource.getConnection()) {
            con.createStatement().executeQuery("SELECT count(*) FROM shutdown_sensitive");
            fail("Custom schema objects should not be exists after the shutdown");
        } catch (SQLException e) {
            // expected Table "SHUTDOWN_SENSITIVE" not found
        }
    }

    @AfterAll
    static /* default */void afterAll() {
        if (dataSourceFactory != null && dataSource != null) {
            dataSourceFactory.dispose(dataSource);
            dataSourceFactory = null;
            dataSource = null;
        }
    }
}
