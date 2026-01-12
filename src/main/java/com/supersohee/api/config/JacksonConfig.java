package com.supersohee.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime을 한국 시간대(Asia/Seoul)로 직렬화
        // 프론트엔드에서 타임존 정보를 명확히 알 수 있도록 ISO 8601 형식에 타임존 포함
        javaTimeModule.addSerializer(LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                // LocalDateTime을 한국 시간대 ZonedDateTime으로 변환
                ZonedDateTime zonedDateTime = value.atZone(SEOUL_ZONE);
                // ISO 8601 형식으로 직렬화 (예: 2025-12-31T19:00:00+09:00)
                gen.writeString(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        });
        
        // 역직렬화: 타임존 정보가 있으면 파싱, 없으면 LocalDateTime으로 처리
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
            @Override
            public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
                String text = p.getText();
                try {
                    // ISO 8601 형식 (타임존 포함) 파싱 시도
                    if (text.contains("+") || text.contains("Z") || (text.contains("-") && text.matches(".*\\d{2}:\\d{2}$"))) {
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        return zonedDateTime.withZoneSameInstant(SEOUL_ZONE).toLocalDateTime();
                    }
                } catch (Exception e) {
                    // 파싱 실패 시 기본 형식으로 처리
                }
                // 기본 형식 파싱 (타임존 없음)
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });

        return builder
                .modules(javaTimeModule)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .timeZone(java.util.TimeZone.getTimeZone(SEOUL_ZONE))
                .build();
    }

    // HTTP 메시지 컨버터에 ObjectMapper 강제 적용
    @Override
    public void extendMessageConverters(List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter jsonConverter = (MappingJackson2HttpMessageConverter) converter;
                // 기존 ObjectMapper를 가져와서 설정 적용
                ObjectMapper mapper = jsonConverter.getObjectMapper();
                if (mapper != null) {
                    JavaTimeModule javaTimeModule = new JavaTimeModule();
                    javaTimeModule.addSerializer(LocalDateTime.class, new com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                        @Override
                        public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider provider) throws java.io.IOException {
                            ZonedDateTime zonedDateTime = value.atZone(SEOUL_ZONE);
                            gen.writeString(zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                        }
                    });
                    mapper.registerModule(javaTimeModule);
                    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                    mapper.setTimeZone(java.util.TimeZone.getTimeZone(SEOUL_ZONE));
                }
            }
        }
    }
}
