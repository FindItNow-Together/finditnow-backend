package com.finditnow.userservice.config;

import com.finditnow.userservice.security.JwtAuthFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://localhost", "http://localhost:3000")
//                .allowedMethods("*")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        loggingFilter.setMaxPayloadLength(64000); // Set max length for payload
        return loggingFilter;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(filter);
        bean.addUrlPatterns("/*");
        bean.setOrder(1); // important
        return bean;
    }
}