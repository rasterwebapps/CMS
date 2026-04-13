package com.cms.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cms.dto.InventoryItemRequest;
import com.cms.dto.InventoryItemResponse;
import com.cms.exception.ResourceNotFoundException;
import com.cms.service.InventoryItemService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = InventoryItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryItemService inventoryItemService;

    @Test
    void shouldCreateInventoryItem() throws Exception {
        InventoryItemRequest request = new InventoryItemRequest(
            "Network Cable", "INV001", 1L, 100, 20, "pcs", "Cat6 cable", LocalDate.now()
        );

        InventoryItemResponse response = createResponse(1L, "Network Cable", 100, 20, false);

        when(inventoryItemService.create(any(InventoryItemRequest.class))).thenReturn(response);

        mockMvc.perform(post("/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Network Cable"));

        verify(inventoryItemService).create(any(InventoryItemRequest.class));
    }

    @Test
    void shouldFindAllItems() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 100, 20, false);

        when(inventoryItemService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(inventoryItemService).findAll();
    }

    @Test
    void shouldFindByLabId() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 100, 20, false);

        when(inventoryItemService.findByLabId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/inventory").param("labId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(inventoryItemService).findByLabId(1L);
    }

    @Test
    void shouldFindLowStockItems() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 10, 20, true);

        when(inventoryItemService.findLowStockItems()).thenReturn(List.of(response));

        mockMvc.perform(get("/inventory").param("lowStockOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lowStock").value(true));

        verify(inventoryItemService).findLowStockItems();
    }

    @Test
    void shouldFindLowStockItemsByLabId() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 10, 20, true);

        when(inventoryItemService.findLowStockItemsByLabId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/inventory").param("labId", "1").param("lowStockOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        verify(inventoryItemService).findLowStockItemsByLabId(1L);
    }

    @Test
    void shouldFindById() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 100, 20, false);

        when(inventoryItemService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/inventory/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(inventoryItemService).findById(1L);
    }

    @Test
    void shouldReturnNotFound() throws Exception {
        when(inventoryItemService.findById(999L))
            .thenThrow(new ResourceNotFoundException("Inventory item not found with id: 999"));

        mockMvc.perform(get("/inventory/999"))
            .andExpect(status().isNotFound());

        verify(inventoryItemService).findById(999L);
    }

    @Test
    void shouldFindByItemCode() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 100, 20, false);

        when(inventoryItemService.findByItemCode("INV001")).thenReturn(response);

        mockMvc.perform(get("/inventory/code/INV001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));

        verify(inventoryItemService).findByItemCode("INV001");
    }

    @Test
    void shouldUpdateInventoryItem() throws Exception {
        InventoryItemRequest request = new InventoryItemRequest(
            "Cat6 Cable", "INV001", 1L, 150, 30, "pcs", "Updated description", null
        );

        InventoryItemResponse response = createResponse(1L, "Cat6 Cable", 150, 30, false);

        when(inventoryItemService.update(eq(1L), any(InventoryItemRequest.class))).thenReturn(response);

        mockMvc.perform(put("/inventory/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cat6 Cable"));

        verify(inventoryItemService).update(eq(1L), any(InventoryItemRequest.class));
    }

    @Test
    void shouldUpdateQuantity() throws Exception {
        InventoryItemResponse response = createResponse(1L, "Network Cable", 110, 20, false);

        when(inventoryItemService.updateQuantity(1L, 10)).thenReturn(response);

        mockMvc.perform(patch("/inventory/1/quantity").param("change", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(110));

        verify(inventoryItemService).updateQuantity(1L, 10);
    }

    @Test
    void shouldDeleteInventoryItem() throws Exception {
        doNothing().when(inventoryItemService).delete(1L);

        mockMvc.perform(delete("/inventory/1"))
            .andExpect(status().isNoContent());

        verify(inventoryItemService).delete(1L);
    }

    @Test
    void shouldReturnNotFoundWhenDeleting() throws Exception {
        doThrow(new ResourceNotFoundException("Inventory item not found with id: 999"))
            .when(inventoryItemService).delete(999L);

        mockMvc.perform(delete("/inventory/999"))
            .andExpect(status().isNotFound());

        verify(inventoryItemService).delete(999L);
    }

    private InventoryItemResponse createResponse(Long id, String name, Integer quantity, Integer minimumQuantity, boolean lowStock) {
        Instant now = Instant.now();
        return new InventoryItemResponse(
            id, name, "INV001", 1L, "Lab 1", quantity, minimumQuantity,
            "pcs", "Description", LocalDate.now(), lowStock, now, now
        );
    }
}
