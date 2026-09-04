package com.marketpulse.watchlist;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketpulse.auth.dto.RegisterRequest;
import com.marketpulse.watchlist.dto.AddStockRequest;
import com.marketpulse.watchlist.dto.WatchlistRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WatchlistIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String name, String email) throws Exception {
        RegisterRequest register = new RegisterRequest(name, email, "password123", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }

    @Test
    void testWatchlistCrudAndStockManagement() throws Exception {
        String token = registerAndGetToken("Alice", "alice.crud@example.com");

        // 1. Create watchlist
        WatchlistRequest createReq = new WatchlistRequest("Tech Giants");
        MvcResult createResult = mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tech Giants"))
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();

        Long watchlistId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Rename watchlist
        WatchlistRequest renameReq = new WatchlistRequest("US Tech Giants");
        mockMvc.perform(put("/api/watchlists/" + watchlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("US Tech Giants"));

        // 3. Add stocks: NVDA, AAPL
        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("NVDA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCount").value(1))
                .andExpect(jsonPath("$.stocks[0].symbol").value("NVDA"));

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("AAPL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCount").value(2));

        // 4. Remove stock: NVDA
        mockMvc.perform(delete("/api/watchlists/" + watchlistId + "/stocks/NVDA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // 5. Verify watchlist detail has 1 stock left (AAPL)
        mockMvc.perform(get("/api/watchlists/" + watchlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockCount").value(1))
                .andExpect(jsonPath("$.stocks[0].symbol").value("AAPL"));

        // 6. Delete watchlist
        mockMvc.perform(delete("/api/watchlists/" + watchlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        // 7. Verify deletion -> 404
        mockMvc.perform(get("/api/watchlists/" + watchlistId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDuplicateWatchlistNameRejected() throws Exception {
        String token = registerAndGetToken("Bob", "bob.dups@example.com");

        WatchlistRequest req = new WatchlistRequest("Growth Stocks");
        mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void testDuplicateStockInWatchlistRejected() throws Exception {
        String token = registerAndGetToken("Charlie", "charlie.dups@example.com");

        MvcResult res = mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistRequest("AI Portfolio"))))
                .andExpect(status().isCreated())
                .andReturn();

        Long watchlistId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Add MSFT first time
        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("MSFT"))))
                .andExpect(status().isOk());

        // Add MSFT second time -> 409 Conflict
        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("MSFT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void testDataOwnershipSecurityUserBCannotAccessUserAWatchlist() throws Exception {
        // User A creates Watchlist
        String tokenA = registerAndGetToken("UserA", "userA@example.com");
        MvcResult resA = mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistRequest("Confidential A"))))
                .andExpect(status().isCreated())
                .andReturn();
        Long watchlistIdA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("id").asLong();

        // User B authenticates
        String tokenB = registerAndGetToken("UserB", "userB@example.com");

        // User B attempts to access User A's watchlist -> 403 Forbidden
        mockMvc.perform(get("/api/watchlists/" + watchlistIdA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("You do not have access to this watchlist"));

        // User B attempts to add stock to User A's watchlist -> 403 Forbidden
        mockMvc.perform(post("/api/watchlists/" + watchlistIdA + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("TSLA"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // User B attempts to rename User A's watchlist -> 403 Forbidden
        mockMvc.perform(put("/api/watchlists/" + watchlistIdA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistRequest("Hacked Name"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // User B attempts to delete User A's watchlist -> 403 Forbidden
        mockMvc.perform(delete("/api/watchlists/" + watchlistIdA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        // Non-existent watchlist returns 404 Not Found (NOT 403)
        mockMvc.perform(get("/api/watchlists/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
