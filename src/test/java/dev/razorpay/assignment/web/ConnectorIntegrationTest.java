package dev.razorpay.assignment.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ConnectorIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void searchReturnsNormalizedTickets() throws Exception { mvc.perform(get("/api/tickets/search?status=open&priority=high")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1001)).andExpect(jsonPath("$[0].subject").value("Payment failed after OTP")); }
    @Test void toolSpecIsReadOnly() throws Exception { mvc.perform(get("/mcp/tools")).andExpect(status().isOk()).andExpect(jsonPath("$.tools[0].readOnly").value(true)).andExpect(jsonPath("$.tools.length()").value(5)); }
    @Test void missingTicketIsStructuredError() throws Exception { mvc.perform(get("/api/tickets/9999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("UPSTREAM_ERROR")); }
}
