package com.github.edwgiz.sample.bank.core.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.github.edwgiz.sample.bank.core.storage.DSLContextFactory;
import com.github.edwgiz.sample.bank.core.storage.InMemoryDataSourceFactory;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.jooq.DSLContext;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Value.construct;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.github.edwgiz.sample.bank.core.webapp.LocalDateTimeParamConverterProvider.DATE_TIME_FORMATTER;

public class JaxRsApplicationFactory implements Supplier<ResourceConfig> {

    /**
     * Pre-configured jax-rs application. Included components:
     * <ol>
     *     <li>{@link org.glassfish.hk2.utilities.Binder} calling local {@link #configure(AbstractBinder)} method.</li>
     *     <li>{@link LocalDateTimeParamConverterProvider} to handle REST query parameters.</li>
     *     <li>{@link ObjectMapper} with a disabled visibility for the fields and the setters.</li>
     *     <li>{@link JavaTimeModule} with {@link LocalDateTimeParamConverterProvider#DATE_TIME_FORMATTER} for json
     *     bodies.</li>
     *     <li>{@link ThrowableExceptionMapper}, switching the html error pages to the text output.</li>
     *     <li>{@link OpenApiResource}</li>
     * </ol>
     *
     * @return JAX-RS config that can be augmented outside.
     */
    @Override
    public ResourceConfig get() {
        final AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                JaxRsApplicationFactory.this.configure(this);
            }
        };

        final ResourceConfig conf = new ResourceConfig();
        conf.register(binder);
        conf.register(new LocalDateTimeParamConverterProvider());

        final JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        final JacksonJaxbJsonProvider jaxbJsonProvider = new JacksonJaxbJsonProvider();
        jaxbJsonProvider.setMapper(new ObjectMapper()
                .setDefaultVisibility(construct(NONE, PUBLIC_ONLY, PUBLIC_ONLY, NONE, PUBLIC_ONLY))
                .registerModule(timeModule)
                .configure(WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(FAIL_ON_EMPTY_BEANS, false));
        conf.register(jaxbJsonProvider);

        conf.register(new ThrowableExceptionMapper());
        conf.register(OpenApiResource.class);

        return conf;
    }

    /**
     * Adds {@link DSLContextFactory}.
     *
     * @param binder binder to configure.
     */
    protected void configure(final AbstractBinder binder) {
        binder.bindFactory(InMemoryDataSourceFactory.class).to(DataSource.class);
        binder.bindFactory(DSLContextFactory.class).to(DSLContext.class);
    }
}
