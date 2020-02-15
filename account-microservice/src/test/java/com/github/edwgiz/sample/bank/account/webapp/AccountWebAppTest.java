package com.github.edwgiz.sample.bank.account.webapp;

import com.github.edwgiz.sample.bank.account.api.AccountEndpoint;
import com.github.edwgiz.sample.bank.account.api.PaymentEndpoint;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountWebAppTest {

    @Test
        /* default */void testCreateResourceConfig() {
        final ResourceConfig conf = new AccountWebApp().createResourceConfig();
        assertEquals("BankAccounts", conf.getApplicationName());
        Assertions.assertTrue(conf.isRegistered(AccountEndpoint.class));
        Assertions.assertTrue(conf.isRegistered(PaymentEndpoint.class));
    }
}
