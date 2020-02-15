package com.github.edwgiz.sample.bank.core.webapp.http;

/**
 * String clone of {@link javax.ws.rs.core.Response.Status}.
 */
public final class ResponseUtils {

    /**
     * @see javax.ws.rs.core.Response.Status#OK
     */
    public static final String OK_CODE = "200";
    /**
     * @see javax.ws.rs.core.Response.Status#NO_CONTENT
     */
    public static final String NO_CONTENT_CODE = "204";
    /**
     * @see javax.ws.rs.core.Response.Status#NOT_MODIFIED
     */
    public static final String NOT_MODIFIED_CODE = "304";
    /**
     * @see javax.ws.rs.core.Response.Status#BAD_REQUEST
     */
    public static final String BAD_REQUEST_CODE = "400";
    /**
     * @see javax.ws.rs.core.Response.Status#CONFLICT
     */
    public static final String CONFLICT_CODE = "409";


    private ResponseUtils() {
    }
}
