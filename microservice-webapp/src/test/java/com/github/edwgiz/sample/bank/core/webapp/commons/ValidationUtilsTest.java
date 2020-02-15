package com.github.edwgiz.sample.bank.core.webapp.commons;

import org.junit.jupiter.api.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationUtilsTest {

    public static void assertWebApplicationException(final WebApplicationException wae, final String expectedMsg) {
        assertTrue(wae.getMessage().endsWith(BAD_REQUEST.getReasonPhrase()));
        assertEquals(expectedMsg, wae.getResponse().getEntity());
        assertEquals(BAD_REQUEST, wae.getResponse().getStatusInfo());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, wae.getResponse().getMediaType());
    }

    @Test
    public void testException() {
        final String msg = "Incorrect account name";
        assertWebApplicationException(ValidationUtils.exception(msg), msg);
    }
}
