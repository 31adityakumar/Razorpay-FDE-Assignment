package dev.razorpay.assignment.model;

import java.util.List;

public record Page<T>(List<T> items, int page, int perPage, boolean hasMore) {}
