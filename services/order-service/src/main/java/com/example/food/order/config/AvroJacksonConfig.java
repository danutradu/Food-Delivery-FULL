package com.example.food.order.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.Module;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hide Avro-specific getters from Jackson to avoid serializing Schema internals. */
@Configuration
public class AvroJacksonConfig {


    /** Ignore typical Avro "bean" properties exposed by SpecificRecordBase. */
    @JsonIgnoreProperties({ "schema", "specificData" })
    public abstract static class AvroSpecificRecordPropsMixin {
        @JsonIgnore public abstract Schema getSchema();
        // If present on your Avro version:
        @JsonIgnore public abstract org.apache.avro.specific.SpecificData getSpecificData();
    }

    /** Ignore internals inside SpecificData that drag Jackson into Avro Schema internals. */
    @JsonIgnoreProperties({ "conversions", "genericData", "classLoader", "stringType" })
    public abstract static class AvroSpecificDataMixin {}

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer avroMixins() {
        return builder -> {
            builder.mixIn(SpecificRecordBase.class, AvroSpecificRecordPropsMixin.class);
            builder.mixIn(org.apache.avro.specific.SpecificData.class, AvroSpecificDataMixin.class);
        };
    }
}
