package com.supersohee.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        
        // LocalDateTime을 Date로 변환 (한국 시간대로 저장)
        converters.add(new LocalDateTimeToDateConverter());
        
        // Date를 LocalDateTime으로 변환 (한국 시간대로 읽기)
        converters.add(new DateToLocalDateTimeConverter());
        
        return new MongoCustomConversions(converters);
    }

    @WritingConverter
    static class LocalDateTimeToDateConverter implements Converter<LocalDateTime, Date> {
        @Override
        public Date convert(LocalDateTime source) {
            // LocalDateTime을 한국 시간대 ZonedDateTime으로 변환 후 Date로 변환
            ZonedDateTime zonedDateTime = source.atZone(ZoneId.of("Asia/Seoul"));
            return Date.from(zonedDateTime.toInstant());
        }
    }

    @ReadingConverter
    static class DateToLocalDateTimeConverter implements Converter<Date, LocalDateTime> {
        @Override
        public LocalDateTime convert(Date source) {
            // Date를 한국 시간대 LocalDateTime으로 변환
            return source.toInstant()
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime();
        }
    }
}
