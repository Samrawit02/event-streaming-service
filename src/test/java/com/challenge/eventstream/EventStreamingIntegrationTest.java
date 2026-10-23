package com.challenge.eventstream;

import com.challenge.eventstream.dto.LoginRequest;
import com.challenge.eventstream.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventStreamingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Authenticate with the seeded admin user
        LoginRequest loginRequest = new LoginRequest("admin", "password123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rootNode = objectMapper.readTree(result.getResponse().getContentAsString());
        this.authToken = rootNode.get("token").asText();
    }

    @Test
    @DisplayName("Unauthenticated requests to protected endpoints must return 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/events/recent"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/events/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"eventType\":\"VIEW\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User registration and subsequent login should succeed")
    void testRegisterAndLogin() throws Exception {
        RegisterRequest register = new RegisterRequest("newuser", "password456");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));

        LoginRequest login = new LoginRequest("newuser", "password456");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @DisplayName("Ingest single event and query recent events from queue")
    void testSingleEventIngestionAndRecent() throws Exception {
        String singleEventJson = """
                {
                    "userId": 101,
                    "eventType": "VIEW",
                    "productId": 5001,
                    "category": "electronics"
                }
                """;

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleEventJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.eventType").value("VIEW"))
                .andExpect(jsonPath("$.category").value("electronics"));

        // Verify recent events endpoint
        mockMvc.perform(get("/events/recent")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(not(empty()))))
                .andExpect(jsonPath("$[0].userId").value(101));
    }

    @Test
    @DisplayName("Ingest batch events and verify summary aggregation")
    void testBatchIngestionAndSummary() throws Exception {
        String batchJson = """
                [
                    { "userId": 201, "eventType": "VIEW", "productId": 1, "category": "books" },
                    { "userId": 202, "eventType": "ADD_TO_CART", "productId": 1, "category": "books" },
                    { "userId": 203, "eventType": "PURCHASE", "productId": 1, "category": "books" },
                    { "userId": 204, "eventType": "VIEW", "productId": 2, "category": "fashion" }
                ]
                """;

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(4)));

        // Summary without category
        mockMvc.perform(get("/events/summary")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.VIEW", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.ADD_TO_CART", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.PURCHASE", greaterThanOrEqualTo(1)));

        // Summary with category 'books'
        mockMvc.perform(get("/events/summary")
                        .param("category", "books")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.VIEW", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ADD_TO_CART", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.PURCHASE", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Filter events by criteria and verify LRU cache HIT on subsequent call")
    void testSearchEventsWithLruCache() throws Exception {
        String event = """
                {
                    "userId": 999,
                    "eventType": "PURCHASE",
                    "productId": 8888,
                    "category": "gaming"
                }
                """;

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event))
                .andExpect(status().isCreated());

        // First search query: expect MISS
        mockMvc.perform(get("/events")
                        .param("userId", "999")
                        .param("category", "gaming")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$[0].userId").value(999))
                .andExpect(jsonPath("$[0].category").value("gaming"));

        // Second identical search query: expect HIT from LRU Cache
        mockMvc.perform(get("/events")
                        .param("userId", "999")
                        .param("category", "gaming")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "HIT"))
                .andExpect(jsonPath("$[0].userId").value(999));
    }

    @Test
    @DisplayName("Validation should reject invalid eventType")
    void testInvalidEventTypeRejected() throws Exception {
        String invalidEvent = """
                {
                    "userId": 1,
                    "eventType": "INVALID_TYPE",
                    "category": "books"
                }
                """;

        mockMvc.perform(post("/events")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEvent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
