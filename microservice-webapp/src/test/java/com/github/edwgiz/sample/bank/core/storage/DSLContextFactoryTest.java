package com.github.edwgiz.sample.bank.core.storage;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


class DSLContextFactoryTest {

    @Test
        /* default */void test() {
        final DataSource dataSource = Mockito.mock(DataSource.class);
        final DSLContextFactory dslCtxFctr = new DSLContextFactory(dataSource);
        Mockito.verifyNoInteractions(dataSource);
        final DSLContext dslCtx = dslCtxFctr.provide();
        Mockito.verifyNoInteractions(dataSource);

        final Configuration conf = dslCtx.configuration();
        assertSame(SQLDialect.H2, conf.dialect());
        assertTrue(conf.settings().isReturnIdentityOnUpdatableRecord());
        assertFalse(conf.settings().isRenderSchema());
        assertTrue(conf.transactionProvider() instanceof ThreadLocalTransactionProvider);

        final DSLContext dslCtxToDispose = Mockito.spy(dslCtx);
        dslCtxFctr.dispose(dslCtxToDispose);
        Mockito.verifyNoInteractions(dslCtxToDispose);
    }
}
