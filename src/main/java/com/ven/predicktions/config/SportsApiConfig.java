package com.ven.predicktions.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SportsApiConfig {

    @Bean
    public RestClient footballDataRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${sports.football-data.api-token}") String apiToken
    ) {
        return restClientBuilder
                .baseUrl("https://api.football-data.org/v4")
                .defaultHeader("X-Auth-Token", apiToken)
                .build();
    }
}