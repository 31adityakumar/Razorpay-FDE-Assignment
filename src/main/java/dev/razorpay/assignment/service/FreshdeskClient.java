package dev.razorpay.assignment.service;

import dev.razorpay.assignment.model.*;
import java.time.Instant;
import java.util.List;

public interface FreshdeskClient {
    Page<Ticket> listTickets(int page, int perPage);
    Ticket getTicket(long id);
    List<Ticket> searchTickets(String status, String priority, Instant createdAfter, String query);
    List<Conversation> conversations(long ticketId);
    List<Contact> searchContacts(String query);
}
