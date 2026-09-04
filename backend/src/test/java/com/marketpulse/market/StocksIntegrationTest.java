package com.marketpulse.market;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketpulse.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
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
class StocksIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest register = new RegisterRequest("StockTester", "stocktester@example.com", "password123", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        token = node.get("token").asText();
    }

    @Test
    void unauthenticatedAccessReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCatalogReturnsSupportedStocksWithQuotes() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(15))))
                .andExpect(jsonPath("$[0].symbol").isNotEmpty())
                .andExpect(jsonPath("$[0].companyName").isNotEmpty())
                .andExpect(jsonPath("$[0].price").isNotEmpty());
    }

    @Test
    void searchBySymbolReturnsMatchingStock() throws Exception {
        mockMvc.perform(get("/api/stocks/search")
                        .param("query", "NVDA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("NVDA"))
                .andExpect(jsonPath("$[0].companyName").value("NVIDIA Corporation"));
    }

    @Test
    void searchByCompanyNameReturnsMatchingStock() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .param("query", "Tesla")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("TSLA"))
                .andExpect(jsonPath("$[0].companyName").value("Tesla, Inc."));
    }

    @Test
    void getSingleStockReturnsQuote() throws Exception {
        mockMvc.perform(get("/api/stocks/AAPL")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.companyName").value("Apple Inc."))
                .andExpect(jsonPath("$.price").isNumber());
    }

    @Test
    void searchWithNoMatchesReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/stocks/search")
                        .param("query", "NonExistentCompanyXYZ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
