package dev.razorpay.assignment.service;

import dev.razorpay.assignment.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@ConditionalOnProperty(name="freshdesk.mock", havingValue="true", matchIfMissing=true)
public class MockFreshdeskClient implements FreshdeskClient {
    private final List<Ticket> tickets = List.of(
        new Ticket(1001, "Payment failed after OTP", "open", "high", 501, Instant.parse("2026-09-29T08:00:00Z"), Instant.parse("2026-09-29T09:00:00Z")),
        new Ticket(1002, "Refund status", "pending", "medium", 502, Instant.parse("2026-09-28T10:00:00Z"), Instant.parse("2026-09-28T11:00:00Z")),
        new Ticket(1003, "Unable to update address", "resolved", "low", 501, Instant.parse("2026-09-20T10:00:00Z"), Instant.parse("2026-09-20T11:00:00Z")));
    public Page<Ticket> listTickets(int page, int perPage) { int from=(page-1)*perPage, to=Math.min(from+perPage,tickets.size()); return new Page<>(from>=tickets.size()?List.of():tickets.subList(from,to), page, perPage, to<tickets.size()); }
    public Ticket getTicket(long id) { return tickets.stream().filter(t->t.id()==id).findFirst().orElseThrow(()->new FreshdeskException("Ticket not found",404)); }
    public List<Ticket> searchTickets(String status,String priority,Instant after,String query) { return tickets.stream().filter(t->status==null||status.equals(t.status())).filter(t->priority==null||priority.equals(t.priority())).filter(t->after==null||t.createdAt().isAfter(after)).filter(t->query==null||t.subject().toLowerCase().contains(query.toLowerCase())).toList(); }
    public List<Conversation> conversations(long id) { getTicket(id); return List.of(new Conversation(9001,id,"We are investigating the payment attempt.","support-agent",Instant.parse("2026-09-29T09:05:00Z"),false)); }
    public List<Contact> searchContacts(String query) { return List.of(new Contact(501,"Asha Customer","asha@example.test",true),new Contact(502,"Bharat Customer","bharat@example.test",true)).stream().filter(c->query==null||c.name().toLowerCase().contains(query.toLowerCase())||c.email().contains(query.toLowerCase())).toList(); }
}
