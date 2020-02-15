package com.github.edwgiz.sample.bank.account.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class NewAccountTest {

    @Test
        /* default */void testUnsupportedOperationException() {
        final NewAccount obj = new NewAccount();
        assertThrows(UnsupportedOperationException.class, obj::getAccountId);
        assertThrows(UnsupportedOperationException.class, obj::getBalance);
        assertThrows(UnsupportedOperationException.class, obj::getBalanceLastModified);
        assertThrows(UnsupportedOperationException.class, obj::getCreated);
        assertThrows(UnsupportedOperationException.class, () -> obj.from(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.into(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setAccountId(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setBalance(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setBalanceLastModified(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setCreated(null));
    }
}
