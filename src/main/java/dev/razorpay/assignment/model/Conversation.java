package dev.razorpay.assignment.model;

import java.time.Instant;

public record Conversation(long id, long ticketId, String body, String author, Instant createdAt, boolean incoming) {}
