package com.github.edwgiz.sample.bank.core.webapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.jaxrs.cfg.JaxRSFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.mockito.ArgumentMatchers.notNull;

class JaxRsApplicationFactoryTest {

    @Test
    public void test() throws IOException {
        final JaxRsApplicationFactory jaxRsApplicationFactory = Mockito.spy(new JaxRsApplicationFactory());
        final ResourceConfig application = jaxRsApplicationFactory.get();
        final ApplicationHandler handler = new ApplicationHandler(application); // do dependency injection
        Mockito.verify(jaxRsApplicationFactory, Mockito.times(1)).get();
        Mockito.verify(jaxRsApplicationFactory, Mockito.times(1)).configure(notNull());
        Mockito.verifyNoMoreInteractions(jaxRsApplicationFactory);

        final InjectionManager injectionManager = handler.getInjectionManager();

        Assertions.assertNotNull(injectionManager.getInstance(DataSource.class));
        Assertions.assertNotNull(injectionManager.getInstance(DSLContext.class));
        Assertions.assertTrue(application.isRegistered(LocalDateTimeParamConverterProvider.class));
        testJacksonJaxbJsonProvider(injectionManager.getInstance(JacksonJaxbJsonProvider.class));
        Assertions.assertTrue(application.isRegistered(ThrowableExceptionMapper.class));
    }

    private void testJacksonJaxbJsonProvider(final JacksonJaxbJsonProvider jaxbProvider)
            throws JsonProcessingException {
        Assertions.assertNotNull(jaxbProvider);
        testJaxbProviderFeatures(jaxbProvider);
        final ObjectMapper javaTimeMapper = jaxbProvider.locateMapper(LocalDateTime.class,
                MediaType.APPLICATION_JSON_TYPE);
        testObjectMapperDefaultVisibility(javaTimeMapper);
        testJavaTimeModule(javaTimeMapper);

        //test FAIL_ON_EMPTY_BEANS, false
        Assertions.assertEquals("{}", javaTimeMapper.writeValueAsString(new Object()));
    }

    private void testObjectMapperDefaultVisibility(final ObjectMapper javaTimeMapper) {
        testObjectMapperDefaultVisibility(javaTimeMapper.getDeserializationConfig().getDefaultVisibilityChecker());
    }

    private void testJavaTimeModule(final ObjectMapper javaTimeMapper) throws JsonProcessingException {
        final LocalDateTime ldt = LocalDateTime.of(2000, 2, 28, 23, 50, 10, 123_456_789);
        final String ldtStr = "\"2000-02-28T23:50:10.123\"";
        Assertions.assertEquals(ldtStr, javaTimeMapper.writeValueAsString(ldt));
        Assertions.assertEquals(ldt.truncatedTo(MILLIS), javaTimeMapper.readValue(ldtStr, LocalDateTime.class));
    }

    private void testJaxbProviderFeatures(final JacksonJaxbJsonProvider jaxbProvider) {
        Assertions.assertFalse(jaxbProvider.isEnabled(JaxRSFeature.ADD_NO_SNIFF_HEADER));
        Assertions.assertTrue(jaxbProvider.isEnabled(JaxRSFeature.ALLOW_EMPTY_INPUT));
        Assertions.assertTrue(jaxbProvider.isEnabled(JaxRSFeature.CACHE_ENDPOINT_READERS));
        Assertions.assertTrue(jaxbProvider.isEnabled(JaxRSFeature.CACHE_ENDPOINT_WRITERS));
        Assertions.assertFalse(jaxbProvider.isEnabled(JaxRSFeature.DYNAMIC_OBJECT_MAPPER_LOOKUP));
        jaxbProvider.checkCanSerialize(true);
        jaxbProvider.checkCanDeserialize(true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void testObjectMapperDefaultVisibility(final VisibilityChecker<?> checker) {
        final Field field = Mockito.mock(Field.class);
        Mockito.doReturn(Modifier.PUBLIC).when(field).getModifiers();
        Assertions.assertFalse(checker.isFieldVisible(field));

        final Method method = Mockito.mock(Method.class);
        Mockito.doReturn(Modifier.PROTECTED).when(method).getModifiers();
        Assertions.assertFalse(checker.isGetterVisible(method));
        Mockito.doReturn(Modifier.PUBLIC).when(method).getModifiers();
        Assertions.assertTrue(checker.isGetterVisible(method));

        Mockito.doReturn(Modifier.PROTECTED).when(method).getModifiers();
        Assertions.assertFalse(checker.isIsGetterVisible(method));
        Mockito.doReturn(Modifier.PUBLIC).when(method).getModifiers();
        Assertions.assertTrue(checker.isIsGetterVisible(method));

        Mockito.doReturn(Modifier.PUBLIC).when(method).getModifiers();
        Assertions.assertFalse(checker.isSetterVisible(method));

        Mockito.doReturn(Modifier.PROTECTED).when(method).getModifiers();
        Assertions.assertFalse(checker.isCreatorVisible(method));
        Mockito.doReturn(Modifier.PUBLIC).when(method).getModifiers();
        Assertions.assertTrue(checker.isCreatorVisible(method));
    }


}
