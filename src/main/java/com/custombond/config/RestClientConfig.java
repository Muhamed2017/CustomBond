package com.custombond.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

     @Value("${app.external.issuepolicy-url}")
    private String externalApiBaseUrl;


    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl(externalApiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }
}