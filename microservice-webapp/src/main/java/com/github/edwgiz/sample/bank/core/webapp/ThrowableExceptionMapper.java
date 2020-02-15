package com.github.edwgiz.sample.bank.core.webapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.fromResponse;
import static javax.ws.rs.core.Response.status;

/**
 * Maps the exceptions of JAX-RS methods to {@link Response}.
 * Allows to change html error pages to the text output.
 */
@Provider
final class ThrowableExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = LoggerFactory.getLogger(ThrowableExceptionMapper.class);

    /**
     * @param throwable a caught exception instance to be translated to {@link Response}.
     * @return response with a json {@code {error=<object>}} format and {@link MediaType#APPLICATION_JSON_TYPE} type.
     */
    @Override
    public Response toResponse(final Throwable throwable) {
        LOG.trace(throwable.getMessage(), throwable);
        Response result;
        if (throwable instanceof WebApplicationException) {
            final Response existing = ((WebApplicationException) throwable).getResponse();
            if (existing.hasEntity()) {
                // We don't suppose to be here
                // When WebApplicationException has a body, it does not go to the custom mappers at all.
                LOG.error("Caught WebApplicationException with a body. ", throwable);
                result = existing;
            } else {
                result = toResponse(fromResponse(existing), throwable.getMessage());
            }
        } else if (throwable instanceof JsonProcessingException) {
            final JsonProcessingException jpex = (JsonProcessingException) throwable;
            String msg = jpex.getOriginalMessage();
            if (jpex.getLocation() != null) {
                msg += " [line " + jpex.getLocation().getLineNr() + ", col " + jpex.getLocation().getColumnNr() + "]";
            }
            result = toResponse(status(BAD_REQUEST), msg);
        } else {
            result = toResponse(status(INTERNAL_SERVER_ERROR), getStackTrace(throwable));
        }

        return result;
    }

    /* default */Response toResponse(final Response.ResponseBuilder builder, final String details) {
        return builder.type(MediaType.TEXT_PLAIN).entity(details).build();
    }


    /* default */String getStackTrace(final Throwable throwable) {
        final StringWriter buff = new StringWriter(16_192);
        throwable.printStackTrace(new PrintWriter(buff));
        return buff.getBuffer().toString();
    }
}
