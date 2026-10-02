package dev.razorpay.assignment.service;

import dev.razorpay.assignment.config.FreshdeskProperties;
import dev.razorpay.assignment.model.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpFreshdeskClientTest {
    @Test
    void mapsFreshdeskFieldsAndHonorsRetryAfter() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://demo.freshdesk.com/api/v2/search/tickets?query=status%3A2+AND+priority%3A3"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "0"));
        server.expect(requestTo("https://demo.freshdesk.com/api/v2/search/tickets?query=status%3A2+AND+priority%3A3"))
            .andRespond(withSuccess("{\"results\":[{\"id\":7,\"subject\":\"Payment failed\",\"status\":2,\"priority\":3,\"requester_id\":42,\"created_at\":\"2026-10-01T10:00:00Z\",\"updated_at\":\"2026-10-01T11:00:00Z\"}],\"total\":1}", org.springframework.http.MediaType.APPLICATION_JSON));

        HttpFreshdeskClient client = new HttpFreshdeskClient(builder.build(), new FreshdeskProperties("demo.freshdesk.com", "key", false, 1, 1000));
        List<Ticket> result = client.searchTickets("open", "high", null, null);

        assertThat(result).singleElement().satisfies(ticket -> {
            assertThat(ticket.id()).isEqualTo(7);
            assertThat(ticket.status()).isEqualTo("open");
            assertThat(ticket.priority()).isEqualTo("high");
            assertThat(ticket.requesterId()).isEqualTo(42);
            assertThat(ticket.createdAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        });
        server.verify();
    }
}
