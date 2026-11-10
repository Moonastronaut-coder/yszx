package com.yunshu.zhixue.platform.time;

import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JsonTimeConfiguration {
    @Bean
    JacksonModule utcMillisModule() {
        var module = new SimpleModule("yunshu-utc-millis");
        module.addSerializer(Instant.class, new ValueSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator, SerializationContext context) {
                generator.writeString(TimePolicy.formatUtc(value));
            }
        });
        return module;
    }
}
