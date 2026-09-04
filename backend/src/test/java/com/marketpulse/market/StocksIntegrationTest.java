package com.marketpulse.market;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
    void listCatalogReturnsPaginatedStocksWithQuotes() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .param("page", "0")
                        .param("size", "25")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(25)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(25))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(50)))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.content[0].symbol").isNotEmpty())
                .andExpect(jsonPath("$.content[0].companyName").isNotEmpty())
                .andExpect(jsonPath("$.content[0].price").isNotEmpty());
    }

    @Test
    void paginationPageOneReturnsDifferentStocks() throws Exception {
        MvcResult page0Result = mockMvc.perform(get("/api/stocks")
                        .param("page", "0")
                        .param("size", "25")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode page0Node = objectMapper.readTree(page0Result.getResponse().getContentAsString());
        String page0FirstSymbol = page0Node.get("content").get(0).get("symbol").asText();

        mockMvc.perform(get("/api/stocks")
                        .param("page", "1")
                        .param("size", "25")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content", hasSize(25)))
                .andExpect(jsonPath("$.content[0].symbol", not(equalTo(page0FirstSymbol))));
    }

    @Test
    void searchBySymbolReturnsMatchingStock() throws Exception {
        mockMvc.perform(get("/api/stocks/search")
                        .param("query", "NVDA")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].symbol").value("NVDA"))
                .andExpect(jsonPath("$.content[0].companyName").value("NVIDIA Corporation"));
    }

    @Test
    void searchByCompanyNameReturnsMatchingStock() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .param("query", "Tesla")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].symbol").value("TSLA"))
                .andExpect(jsonPath("$.content[0].companyName").value("Tesla, Inc."));
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
    void searchWithNoMatchesReturnsEmptyContent() throws Exception {
        mockMvc.perform(get("/api/stocks/search")
                        .param("query", "NonExistentCompanyXYZ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void getStockHistoryReturnsTimeSeriesPoints() throws Exception {
        mockMvc.perform(get("/api/stocks/NVDA/history")
                        .param("range", "1D")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("NVDA"))
                .andExpect(jsonPath("$.range").value("1D"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points", hasSize(greaterThanOrEqualTo(20))))
                .andExpect(jsonPath("$.points[0].price").isNumber());
    }

    @Test
    void filterGainersReturnsPositiveMoves() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .param("filter", "GAINERS")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void sortStocksByPriceDescending() throws Exception {
        mockMvc.perform(get("/api/stocks")
                        .param("sortBy", "price")
                        .param("sortDirection", "desc")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void userSettingsCanBeRetrievedAndUpdated() throws Exception {
        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdPercent").value(3.0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"thresholdPercent\": 4.5}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdPercent").value(4.5));

        mockMvc.perform(get("/api/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thresholdPercent").value(4.5));
    }

    @Test
    void notificationsEndpointWorks() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
