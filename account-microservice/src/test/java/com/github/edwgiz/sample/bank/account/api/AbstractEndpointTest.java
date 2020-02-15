package com.github.edwgiz.sample.bank.account.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.github.edwgiz.sample.bank.account.jooq.tables.pojos.Account;
import com.github.edwgiz.sample.bank.core.webapp.JaxRsApplicationFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractEndpointTest extends JerseyTest {

    private ObjectMapper json;

    /**
     * @return A mapper from inside of a testing jax-rs application.
     */
    public final ObjectMapper getJson() {
        return json;
    }

    protected final Predicate<LocalDateTime> wideTo(final LocalDateTime value, final long delta,
            final ChronoUnit unit) {
        return between(value.minus(delta, unit), value.plus(delta, unit));
    }

    protected final Predicate<LocalDateTime> between(final LocalDateTime intervalFrom, final LocalDateTime intervalTo) {
        return (t) -> t != null && intervalFrom.compareTo(t) <= 0 && t.compareTo(intervalTo) <= 0;
    }

    protected final String format(final LocalDateTime value) {
        final String str;
        try {
            str = json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return str.substring(1, str.length() - 1);
    }


    @Inject
    /* default */ final void setJacksonJaxbJsonProvider(final JacksonJaxbJsonProvider jsonProvider) {
        json = jsonProvider.locateMapper(Account.class, APPLICATION_JSON_TYPE);
    }

    /**
     * @return jax-rs application config to be augmented in the descendants.
     */
    protected ResourceConfig configure() {
        final ResourceConfig conf = new JaxRsApplicationFactory().get();
        conf.register(this);
        return conf;
    }

    /**
     * @param operation operation, representing jax rs-call.
     * @param requestBody request body in json format to be passed to the {@code operation}.
     * @param expectedStatus status to validate a response taken from the {@code operation}.
     * @param expectedMediaType media type to validate a response taken from the {@code operation}.
     */
    protected void testSimpleResponse(final Function<String, Response> operation,
                                      final String requestBody, final Response.Status expectedStatus,
                                      final MediaType expectedMediaType) {
        final Response resp = operation.apply(requestBody);
        assertEquals(expectedStatus, resp.getStatusInfo());
        assertEquals(expectedMediaType, resp.getMediaType());
        if (resp.getMediaType() == null) {
            assertEquals(0, resp.getLength());
        } else {
            assertTrue(resp.hasEntity());
        }
    }
}
