package dev.razorpay.assignment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@EnableConfigurationProperties(FreshdeskProperties.class)
public class AppConfig {
    @Bean RestClient freshdeskRestClient(RestClient.Builder builder, FreshdeskProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.requestTimeoutMs());
        factory.setReadTimeout(properties.requestTimeoutMs());
        return builder.requestFactory(factory).build();
    }
}
