package com.github.edwgiz.sample.bank.core.storage;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.glassfish.hk2.api.Factory;
import org.h2.jdbcx.JdbcDataSource;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


/**
 * Creates in-memory JDBC connection pool.
 */
@Singleton
public final class InMemoryDataSourceFactory implements Factory<DataSource> {

    /**
     * Timeout is particularly for a locking via  SELECT FOR UPDATE  operations.
     */
    /* default */static final long EXCLUSIVE_LOCK_MILLIS = 1000;
    private static final long H2_EXCLUSIVE_LOCK_MULTIPLIER = 3L;


    @Override
    public DataSource provide() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:app;"
                + "DB_CLOSE_DELAY=-1;"
                + "LOCK_TIMEOUT=" + (EXCLUSIVE_LOCK_MILLIS / H2_EXCLUSIVE_LOCK_MULTIPLIER + 1L)); // workaround for H2
        // default transaction isolation level is TRANSACTION_READ_COMMITTED, that's ok

        final Flyway flyway = createFlywayConfiguration().dataSource(dataSource).load();
        flyway.migrate();

        return dataSource;
    }

    /* default */FluentConfiguration createFlywayConfiguration() {
        return Flyway.configure();
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Override
    public void dispose(final DataSource dataSource) {
        try (Connection con = dataSource.getConnection()) {
            con.createStatement().executeUpdate("SHUTDOWN");
        } catch (SQLException e) {
            // ignore
        }
    }
}
