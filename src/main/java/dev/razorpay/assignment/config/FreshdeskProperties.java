package dev.razorpay.assignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "freshdesk")
public record FreshdeskProperties(String domain, String apiKey, boolean mock, int maxRetries, int requestTimeoutMs) {}
