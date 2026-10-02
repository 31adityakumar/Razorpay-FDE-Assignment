package dev.razorpay.assignment.service;

public class FreshdeskException extends RuntimeException {
    private final int status;
    public FreshdeskException(String message, int status) { super(message); this.status = status; }
    public int status() { return status; }
}
