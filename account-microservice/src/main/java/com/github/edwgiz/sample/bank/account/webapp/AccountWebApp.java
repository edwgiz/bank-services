package com.github.edwgiz.sample.bank.account.webapp;

import com.github.edwgiz.sample.bank.account.api.AccountEndpoint;
import com.github.edwgiz.sample.bank.account.api.PaymentEndpoint;
import com.github.edwgiz.sample.bank.core.commons.ExcludeFromJacocoMetrics;
import com.github.edwgiz.sample.bank.core.webapp.WebAppBase;
import org.glassfish.jersey.server.ResourceConfig;

public class AccountWebApp extends WebAppBase {

    /**
     * Instantiates an instance of itself and starts it.
     *
     * @param args ignored arguments
     */
    @ExcludeFromJacocoMetrics.Generated()
    public static void main(final String[] args) {
        new AccountWebApp().start();
    }

    /**
     * @return a complete config of jax-rs application.
     */
    @Override
    protected ResourceConfig createResourceConfig() {
        final ResourceConfig conf = super.createResourceConfig();
        conf.setApplicationName("BankAccounts");

        conf.register(AccountEndpoint.class);
        conf.register(PaymentEndpoint.class);

        return conf;
    }
}
