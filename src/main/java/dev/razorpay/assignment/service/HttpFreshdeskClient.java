package dev.razorpay.assignment.service;

import dev.razorpay.assignment.config.FreshdeskProperties;
import dev.razorpay.assignment.model.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@ConditionalOnProperty(name = "freshdesk.mock", havingValue = "false")
public class HttpFreshdeskClient implements FreshdeskClient {
    private final RestClient http;
    private final FreshdeskProperties properties;

    public HttpFreshdeskClient(RestClient http, FreshdeskProperties properties) {
        this.http = http;
        this.properties = properties;
    }

    private <T> T get(String path, Class<T> type) {
        String domain = Objects.requireNonNull(properties.domain(), "Freshdesk domain is required");
        String apiKey = Objects.requireNonNull(properties.apiKey(), "Freshdesk API key is required");
        Class<T> responseType = Objects.requireNonNull(type, "Freshdesk response type is required");
        int attempt = 0;
        long delayMs = 250;
        while (true) {
            try {
                T body = http.get().uri(URI.create("https://" + domain + "/api/v2" + path))
                    .headers(headers -> headers.setBasicAuth(apiKey, "X"))
                    .retrieve().body(responseType);
                return Objects.requireNonNull(body, "Freshdesk returned an empty response");
            } catch (org.springframework.web.client.HttpStatusCodeException error) {
                int status = error.getStatusCode().value();
                boolean retryable = status == 429 || error.getStatusCode().is5xxServerError();
                if (!retryable || attempt++ >= properties.maxRetries()) {
                    throw new FreshdeskException("Freshdesk request failed: " + error.getStatusText(), status);
                }
                sleepBeforeRetry(error, delayMs);
                delayMs *= 2;
            }
        }
    }

    private void sleepBeforeRetry(org.springframework.web.client.HttpStatusCodeException error, long fallbackMs) {
        String retryAfter = Optional.ofNullable(error.getResponseHeaders())
            .map(headers -> headers.getFirst("Retry-After")).orElse("");
        long waitMs = fallbackMs;
        if (!retryAfter.isBlank()) {
            try { waitMs = Long.parseLong(retryAfter) * 1000; }
            catch (NumberFormatException ignored) { /* use exponential delay */ }
        }
        try { Thread.sleep(waitMs); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new FreshdeskException("Interrupted while waiting to retry Freshdesk", 503);
        }
    }

    public Page<Ticket> listTickets(int page, int perPage) {
        TicketDto[] response = get("/tickets?page=" + page + "&per_page=" + perPage, TicketDto[].class);
        List<Ticket> tickets = Arrays.stream(response).map(this::ticket).toList();
        return new Page<>(tickets, page, perPage, response.length == perPage);
    }

    public Ticket getTicket(long id) { return ticket(get("/tickets/" + id, TicketDto.class)); }

    public List<Ticket> searchTickets(String status, String priority, Instant createdAfter, String query) {
        List<String> terms = new ArrayList<>();
        if (status != null) terms.add("status:" + statusCode(status));
        if (priority != null) terms.add("priority:" + priorityCode(priority));
        if (createdAfter != null) terms.add("created_at:>'" + createdAfter + "'");
        if (query != null && !query.isBlank()) terms.add("subject:'" + query.replace("'", "\\'") + "'");
        if (terms.isEmpty()) return listTickets(1, 100).items();

        String encoded = URLEncoder.encode(String.join(" AND ", terms), StandardCharsets.UTF_8);
        SearchResponse response = get("/search/tickets?query=" + encoded, SearchResponse.class);
        return response.results() == null ? List.of() : Arrays.stream(response.results()).map(this::ticket).toList();
    }

    public List<Conversation> conversations(long ticketId) {
        ConversationDto[] response = get("/tickets/" + ticketId + "/conversations", ConversationDto[].class);
        return Arrays.stream(response).map(c -> new Conversation(c.id(), ticketId, c.bodyText(), "user:" + c.userId(), c.createdAt(), c.incoming())).toList();
    }

    public List<Contact> searchContacts(String query) {
        SearchContactsResponse response = get("/search/contacts?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8), SearchContactsResponse.class);
        return response.results() == null ? List.of() : Arrays.stream(response.results()).map(c -> new Contact(c.id(), c.name(), c.email(), c.active())).toList();
    }

    private Ticket ticket(TicketDto t) { return new Ticket(t.id(), t.subject(), statusName(t.status()), priorityName(t.priority()), t.requesterId(), t.createdAt(), t.updatedAt()); }
    private int statusCode(String value) { return switch (value.toLowerCase(Locale.ROOT)) { case "open" -> 2; case "pending" -> 3; case "resolved" -> 4; case "closed" -> 5; default -> throw new IllegalArgumentException("Unsupported Freshdesk status: " + value); }; }
    private int priorityCode(String value) { return switch (value.toLowerCase(Locale.ROOT)) { case "low" -> 1; case "medium" -> 2; case "high" -> 3; case "urgent" -> 4; default -> throw new IllegalArgumentException("Unsupported Freshdesk priority: " + value); }; }
    private String statusName(Integer value) { return switch (value == null ? 0 : value) { case 2 -> "open"; case 3 -> "pending"; case 4 -> "resolved"; case 5 -> "closed"; default -> "unknown"; }; }
    private String priorityName(Integer value) { return switch (value == null ? 0 : value) { case 1 -> "low"; case 2 -> "medium"; case 3 -> "high"; case 4 -> "urgent"; default -> "unknown"; }; }

    private record TicketDto(long id, String subject, Integer status, Integer priority,
                             @JsonProperty("requester_id") long requesterId,
                             @JsonProperty("created_at") Instant createdAt,
                             @JsonProperty("updated_at") Instant updatedAt) {}
    private record SearchResponse(TicketDto[] results, int total) {}
    private record ConversationDto(long id, @JsonProperty("body_text") String bodyText,
                                   @JsonProperty("user_id") long userId,
                                   @JsonProperty("created_at") Instant createdAt, boolean incoming) {}
    private record ContactDto(long id, String name, String email, boolean active) {}
    private record SearchContactsResponse(ContactDto[] results, int total) {}
}
