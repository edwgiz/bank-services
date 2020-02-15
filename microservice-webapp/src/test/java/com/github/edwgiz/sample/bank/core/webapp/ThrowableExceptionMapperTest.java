package com.github.edwgiz.sample.bank.core.webapp;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThrowableExceptionMapperTest {

    private static final Map<String, MediaType> EXPECTED_HEADERS = singletonMap("Content-Type", TEXT_PLAIN_TYPE);
    private static ThrowableExceptionMapper teh;

    @BeforeAll
    static /* default */void beforeAll() {
        teh = new ThrowableExceptionMapper();
    }


    @Test
    public void testWebApplicationException() {
        final String entity = "test entity";
        assertResponse(
                teh.toResponse(new WebApplicationException("test1",
                        Response.status(NOT_ACCEPTABLE).entity(entity).build())),
                NOT_ACCEPTABLE,
                entity,
                emptyMap());

        assertResponse(
                teh.toResponse(new WebApplicationException("test exception message",
                        Response.status(NOT_MODIFIED).build())),
                NOT_MODIFIED,
                "test exception message",
                EXPECTED_HEADERS);
    }


    @SuppressWarnings("checkstyle:MagicNumber")
    private static Object[][] getJsonProcessingExceptions() {
        return new Object[][]{
                {"json parsing failure 1", null, "json parsing failure 1"},
                {"json parsing failure 2", new JsonLocation(null, -1, 10, 20),
                        "json parsing failure 2 [line 10, col 20]"}};
    }

    @ParameterizedTest
    @MethodSource("getJsonProcessingExceptions")
        /* default */void testJsonProcessingException(final String exceptionMessage, final JsonLocation loc,
            final String expectedEntity) {
        assertResponse(teh.toResponse(new JsonProcessingException(exceptionMessage, loc) {
                }),
                BAD_REQUEST, expectedEntity, EXPECTED_HEADERS);
    }


    private static Object[] getDefaultExceptions() {
        return new Object[]{
                new RuntimeException("test"),
                new Throwable("test"),
                new ExceptionInInitializerError("test"),
                new IOException("test")};
    }

    @ParameterizedTest
    @MethodSource("getDefaultExceptions")
        /* default */void testDefaultExceptionHandling(final Throwable throwable) {
        final StringWriter entity = new StringWriter(16_192);
        throwable.printStackTrace(new PrintWriter(entity));

        assertResponse(teh.toResponse(throwable), INTERNAL_SERVER_ERROR, entity.toString(), EXPECTED_HEADERS);
    }

    private void assertResponse(final Response response, final Response.Status expectedStatus,
            final String expectedEntity, final Map<String, MediaType> expectedHeaders) {
        assertEquals(0, response.getAllowedMethods().size());
        assertEquals(0, response.getCookies().size());
        assertNull(response.getDate());
        assertEquals(expectedEntity, response.getEntity());
        assertNull(response.getEntityTag());
        assertNull(response.getLanguage());
        assertNull(response.getLastModified());
        assertEquals(-1, response.getLength());
        assertEquals(0, response.getLinks().size());
        assertNull(response.getLocation());
        assertEquals(expectedHeaders.get("Content-Type"), response.getMediaType());

        assertEquals(expectedStatus.getStatusCode(), response.getStatus());
        assertSame(expectedStatus, response.getStatusInfo().toEnum());
        assertSame(expectedStatus.getReasonPhrase(), response.getStatusInfo().getReasonPhrase());

        assertEquals(expectedHeaders.size(), response.getHeaders().size());
        assertEquals(expectedHeaders.size(), response.getMetadata().size());
        assertEquals(expectedHeaders.size(), response.getStringHeaders().size());

        for (final Map.Entry<String, MediaType> expectedHeader : expectedHeaders.entrySet()) {
            final MediaType mediaType = expectedHeader.getValue();
            final String key = expectedHeader.getKey();
            assertEquals(mediaType, response.getHeaders().get(key).get(0));
            assertEquals(mediaType, response.getMetadata().get(key).get(0));
            assertEquals(mediaType.toString(), response.getHeaderString(key));
        }
    }

    @AfterAll
    static /* default */void afterAll() {
        if (teh != null) {
            teh = null;
        }
    }
}
