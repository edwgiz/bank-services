package com.github.edwgiz.sample.bank.core.storage;

import org.glassfish.hk2.api.Factory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.ThreadLocalTransactionProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Singleton
public final class DSLContextFactory implements Factory<DSLContext> {

    private final DataSource dataSource;

    /**
     * @param value data source to become underlying.
     */
    @Inject
    public DSLContextFactory(final DataSource value) {
        this.dataSource = value;
    }

    @Override
    public DSLContext provide() {
        final DefaultConfiguration conf = new DefaultConfiguration();
        conf.setSQLDialect(SQLDialect.H2);
        conf.setDataSource(dataSource);
        conf.settings().setReturnIdentityOnUpdatableRecord(true);
        conf.settings().setRenderSchema(false);
        conf.setTransactionProvider(new ThreadLocalTransactionProvider(conf.connectionProvider()));
        return DSL.using(conf);
    }

    @Override
    public void dispose(final DSLContext dslContext) {
    }
}
