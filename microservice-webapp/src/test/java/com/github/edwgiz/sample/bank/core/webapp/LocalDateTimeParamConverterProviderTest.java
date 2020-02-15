package com.github.edwgiz.sample.bank.core.webapp;

import org.junit.jupiter.api.Test;

import javax.ws.rs.ext.ParamConverter;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalDateTimeParamConverterProviderTest {

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void test() {
        final LocalDateTimeParamConverterProvider converterProvider = new LocalDateTimeParamConverterProvider();
        assertNull(converterProvider.getConverter(Object.class, null, null));
        assertNull(converterProvider.getConverter(UUID.class, null, null));
        final ParamConverter<LocalDateTime> converter = converterProvider.getConverter(LocalDateTime.class, null, null);
        assertNotNull(converter);
        assertEquals(
                LocalDateTime.of(2020, 2, 2, 2, 2, 2, 222_000_000),
                converter.fromString("2020-02-02T02:02:02.222"));
        assertEquals(
                "1915-05-15T15:15:15.555",
                converter.toString(LocalDateTime.of(1915, 5, 15, 15, 15, 15, 555_000_000))
        );
    }
}
