package com.github.edwgiz.sample.bank.core.webapp;

import javax.inject.Singleton;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;

/**
 * Example of custom parameter converter for JAX-RS.
 * Converts the dates using {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
 */
@Provider
final class LocalDateTimeParamConverterProvider implements ParamConverterProvider {

    /**
     * Must be set respectively to the database TIMESTAMP precision.
     */
    private static final int FRACTION_LENGTH_FOR_SECONDS = 3;

    /**
     * Similar to {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} but having millis precision (3 numbers after the dot).
     */
    @SuppressWarnings("checkstyle:MagicNumber")
    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, FRACTION_LENGTH_FOR_SECONDS, true)
            .toFormatter();


    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> aClass, final Type type, final Annotation[] annotations) {
        @SuppressWarnings({"rawtypes"})
        final ParamConverter result;
        if (LocalDateTime.class.isAssignableFrom(aClass)) {
            result = new LocalDateTimeParamConverter();
        } else {
            result = null;
        }
        return result;
    }

    @Singleton
    private static final class LocalDateTimeParamConverter implements ParamConverter<LocalDateTime> {
        @Override
        public LocalDateTime fromString(final String str) {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public String toString(final LocalDateTime value) {
            return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
        }
    }
}
