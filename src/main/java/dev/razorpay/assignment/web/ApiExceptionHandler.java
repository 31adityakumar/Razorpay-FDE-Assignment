package dev.razorpay.assignment.web;

import dev.razorpay.assignment.service.FreshdeskException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(FreshdeskException.class) ResponseEntity<?> freshdesk(FreshdeskException e) { return ResponseEntity.status(e.status()).body(Map.of("error","UPSTREAM_ERROR","message",e.getMessage(),"retryable",e.status()==429||e.status()>=500,"timestamp",Instant.now().toString())); }
    @ExceptionHandler(Exception.class) ResponseEntity<?> other(Exception e) { return ResponseEntity.badRequest().body(Map.of("error","INVALID_REQUEST","message",e.getMessage()==null?"Request failed":e.getMessage())); }
}
