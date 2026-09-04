package com.marketpulse.change;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketpulse.auth.dto.RegisterRequest;
import com.marketpulse.change.repository.ChangeEventRepository;
import com.marketpulse.change.repository.UserCheckpointRepository;
import com.marketpulse.market.entity.MarketSnapshot;
import com.marketpulse.market.repository.MarketSnapshotRepository;
import com.marketpulse.watchlist.dto.AddStockRequest;
import com.marketpulse.watchlist.dto.WatchlistRequest;
import java.util.Optional;
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
class ChangeDetectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MarketSnapshotRepository marketSnapshotRepository;

    @Autowired
    private UserCheckpointRepository userCheckpointRepository;

    @Autowired
    private ChangeEventRepository changeEventRepository;

    @Test
    void testChangeDetectionAndCheckpointLifecycle() throws Exception {
        // 1. Register and authenticate user
        RegisterRequest reg = new RegisterRequest("David", "david.change@example.com", "password123", "password123");
        MvcResult authRes = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode authNode = objectMapper.readTree(authRes.getResponse().getContentAsString());
        String token = authNode.get("token").asText();
        Long userId = authNode.get("user").get("id").asLong();

        // 2. Create watchlist and add stocks NVDA, TSLA, AAPL
        MvcResult wlRes = mockMvc.perform(post("/api/watchlists")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistRequest("Tech Movers"))))
                .andExpect(status().isCreated())
                .andReturn();
        Long watchlistId = objectMapper.readTree(wlRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("NVDA"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("TSLA"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/watchlists/" + watchlistId + "/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStockRequest("AAPL"))))
                .andExpect(status().isOk());

        // 3. Initial change detection before any checkpoint (baseline inferred)
        MvcResult initialChanges = mockMvc.perform(get("/api/changes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCheckedAt").doesNotExist())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        JsonNode initialNode = objectMapper.readTree(initialChanges.getResponse().getContentAsString());
        JsonNode items = initialNode.get("items");
        assertTrue(items.size() >= 1);

        // Verify items are sorted by attentionScore descending
        if (items.size() > 1) {
            int score1 = items.get(0).get("attentionScore").asInt();
            int score2 = items.get(1).get("attentionScore").asInt();
            assertTrue(score1 >= score2, "Attention items should be ranked in descending order of attention score");
        }

        // Verify explanation reasons and severity exist
        JsonNode firstItem = items.get(0);
        assertNotNull(firstItem.get("severity").asText());
        assertNotNull(firstItem.get("reasons"));
        assertTrue(firstItem.get("reasons").isArray());

        // 4. Acknowledge checkpoint (POST /api/checkpoints)
        MvcResult checkpointRes = mockMvc.perform(post("/api/checkpoints")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCheckedAt").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Checkpoint updated after a successful review."))
                .andReturn();

        // Verify snapshots are persisted for user's symbols
        Optional<MarketSnapshot> nvdaSnapshot = marketSnapshotRepository.findTopByUserIdAndSymbolOrderByCapturedAtDesc(userId, "NVDA");
        assertTrue(nvdaSnapshot.isPresent(), "Market snapshot should be saved for NVDA on checkpoint update");
        assertNotNull(nvdaSnapshot.get().getPrice());

        // Verify user checkpoint is saved
        assertTrue(userCheckpointRepository.findByUserId(userId).isPresent());

        // Verify unacknowledged change events were acknowledged
        assertTrue(changeEventRepository.findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(userId).isEmpty());

        // 5. Subsequent GET /api/changes now reflects lastCheckedAt from checkpoint
        mockMvc.perform(get("/api/changes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastCheckedAt").isNotEmpty())
                .andExpect(jsonPath("$.items").isArray());
    }
}
