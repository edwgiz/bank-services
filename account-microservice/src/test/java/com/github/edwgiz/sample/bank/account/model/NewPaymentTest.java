package com.github.edwgiz.sample.bank.account.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class NewPaymentTest {

    @Test
        /* default */void testUnsupportedOperationException() {
        final NewPayment obj = new NewPayment();
        assertThrows(UnsupportedOperationException.class, obj::getPaymentId);
        assertThrows(UnsupportedOperationException.class, obj::getProcessed);
        assertThrows(UnsupportedOperationException.class, () -> obj.from(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.into(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setPaymentId(null));
        assertThrows(UnsupportedOperationException.class, () -> obj.setProcessed(null));
    }
}
