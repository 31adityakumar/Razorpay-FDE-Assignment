package dev.razorpay.assignment.model;

import java.time.Instant;

public record Ticket(long id, String subject, String status, String priority, long requesterId, Instant createdAt, Instant updatedAt) {}
