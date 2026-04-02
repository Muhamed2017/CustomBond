package com.custombond;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// To Exclude Repositories
//@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) 
@SpringBootApplication
@EnableAsync
public class CustomBondApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomBondApplication.class, args);
    }
@Bean
public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    restTemplate.setErrorHandler(new ResponseErrorHandler() {
        @Override
        public boolean hasError(ClientHttpResponse response) {
            return false;
        }

        @Override
        public void handleError(ClientHttpResponse response) {
        }
    });

    return restTemplate;
}
}
