package com.bruno.inventory.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Runs the real stack (controllers, services, JPA, Flyway migrations) against in-memory H2. */
@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void itemCrudLifecycle() throws Exception {
        String sku = uniqueSku();
        long id = createItem(sku, 10);

        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].sku", hasItem(sku)));

        mvc.perform(put("/api/items/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(sku, "Renamed", 25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.quantity").value(25));

        mvc.perform(delete("/api/items/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void skuMustBeUniqueIgnoringCase() throws Exception {
        String sku = uniqueSku();
        createItem(sku, 1);

        postJson("/api/items", itemJson(sku.toLowerCase(), "Duplicate", 1))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidItemReturnsFieldErrors() throws Exception {
        postJson("/api/items", itemJson("", "Bad", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void shipmentFlowUpdatesStock() throws Exception {
        long itemId = createItem(uniqueSku(), 10);
        long shipmentId = createShipment();

        String body = postJson("/api/shipments/" + shipmentId + "/items", addItemJson(itemId, 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnits").value(4))
                .andReturn().getResponse().getContentAsString();
        long lineId = ((Number) JsonPath.read(body, "$.items[0].id")).longValue();
        expectStock(itemId, 6);

        postJson("/api/shipments/" + shipmentId + "/items", addItemJson(itemId, 7))
                .andExpect(status().isUnprocessableContent());
        expectStock(itemId, 6);

        mvc.perform(delete("/api/shipments/{id}/items/{lineId}", shipmentId, lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        expectStock(itemId, 10);

        postJson("/api/shipments/" + shipmentId + "/items", addItemJson(itemId, 10)).andExpect(status().isOk());
        mvc.perform(post("/api/shipments/{id}/ship", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
        expectStock(itemId, 0);

        mvc.perform(post("/api/shipments/{id}/cancel", shipmentId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/items/{id}", itemId)).andExpect(status().isConflict());
    }

    @Test
    void cancellingAShipmentRestoresStock() throws Exception {
        long itemId = createItem(uniqueSku(), 8);
        long shipmentId = createShipment();
        postJson("/api/shipments/" + shipmentId + "/items", addItemJson(itemId, 8)).andExpect(status().isOk());
        expectStock(itemId, 0);

        mvc.perform(post("/api/shipments/{id}/cancel", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        expectStock(itemId, 8);
    }

    private long createItem(String sku, int quantity) throws Exception {
        String body = postJson("/api/items", itemJson(sku, "Test item", quantity))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createShipment() throws Exception {
        String body = postJson("/api/shipments", "{\"destination\": \"Store 42, Campinas\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void expectStock(long itemId, int quantity) throws Exception {
        mvc.perform(get("/api/items/{id}", itemId)).andExpect(jsonPath("$.quantity").value(quantity));
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String itemJson(String sku, String name, int quantity) {
        return """
                {"sku": "%s", "name": "%s", "description": "test", "quantity": %d, "unitPrice": 9.90}
                """.formatted(sku, name, quantity);
    }

    private static String addItemJson(long itemId, int quantity) {
        return """
                {"inventoryItemId": %d, "quantity": %d}
                """.formatted(itemId, quantity);
    }

    private static String uniqueSku() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
