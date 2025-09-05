package com.example.reactive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ConvertersConfig {

    @ReadingConverter
    static class RolesReadConverter implements Converter<String, Set<String>> {
        @Override
        public Set<String> convert(String source) {
            if (source == null || source.isBlank()) return Set.of();
            return Arrays.stream(source.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    @WritingConverter
    static class RolesWriteConverter implements Converter<Set<String>, String> {
        @Override
        public String convert(Set<String> source) {
            if (source == null || source.isEmpty()) return "";
            return String.join(",", source);
        }
    }

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return new R2dbcCustomConversions(StoreConversions.NONE, List.of(
                new RolesReadConverter(), new RolesWriteConverter()
        ));
    }
}