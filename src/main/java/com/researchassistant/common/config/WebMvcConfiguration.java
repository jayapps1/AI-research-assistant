package com.researchassistant.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(ApiSecurityProperties.class)
public class WebMvcConfiguration implements WebMvcConfigurer {
    private final ApiSecurityProperties properties;

    public WebMvcConfiguration(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setMaxPageSize(properties.maxPageSize());
        resolver.setFallbackPageable(PageRequest.of(0, Math.min(20, properties.maxPageSize()), Sort.unsorted()));
        resolvers.add(resolver);
    }
}
