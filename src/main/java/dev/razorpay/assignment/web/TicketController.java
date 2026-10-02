package dev.razorpay.assignment.web;

import dev.razorpay.assignment.model.*;
import dev.razorpay.assignment.service.FreshdeskClient;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController @RequestMapping("/api") @Validated
public class TicketController {
    private final FreshdeskClient client;
    public TicketController(FreshdeskClient client) { this.client=client; }
    @GetMapping("/tickets") public Page<Ticket> list(@RequestParam(defaultValue="1") @Min(1) int page, @RequestParam(defaultValue="30") @Min(1) @Max(100) int perPage) { return client.listTickets(page,perPage); }
    @GetMapping("/tickets/{id}") public Ticket get(@PathVariable @Min(1) long id) { return client.getTicket(id); }
    @GetMapping("/tickets/search") public List<Ticket> search(@RequestParam(required=false) String status,@RequestParam(required=false) String priority,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdAfter,@RequestParam(required=false) String query) { return client.searchTickets(status,priority,createdAfter,query); }
    @GetMapping("/tickets/{id}/conversations") public List<Conversation> conversations(@PathVariable @Min(1) long id) { return client.conversations(id); }
    @GetMapping("/contacts/search") public List<Contact> contacts(@RequestParam String query) { return client.searchContacts(query); }
}
