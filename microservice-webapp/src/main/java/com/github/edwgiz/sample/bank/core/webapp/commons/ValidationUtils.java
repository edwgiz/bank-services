package com.github.edwgiz.sample.bank.core.webapp.commons;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public final class ValidationUtils {

    /**
     * @param status status to include into {@link Response} of the returning exception.
     * @param msg message for an exception and for {@link Response} entity.
     * @return new exception.
     */
    public static WebApplicationException exception(final Response.Status status, final CharSequence msg) {
        return new WebApplicationException(Response.status(status).type(TEXT_PLAIN)
                .entity(msg.toString()).build());
    }

    /**
     * @param msg message for an exception and for {@link Response} entity.
     * @return new exception with a {@link Response.Status#BAD_REQUEST} status.
     */
    public static WebApplicationException exception(final CharSequence msg) {
        return exception(BAD_REQUEST, msg);
    }

    private ValidationUtils() {
    }
}
