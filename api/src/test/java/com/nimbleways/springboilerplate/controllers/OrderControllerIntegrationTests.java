package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void processesMixedOrderAndReturnsItsId() throws Exception {
        LocalDate today = LocalDate.now();
        List<Product> products = List.of(
                new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null),
                new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null),
                new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter", today.plusDays(26), null, null),
                new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk", today.minusDays(2), null, null),
                new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon", null,
                        today.minusDays(2), today.plusDays(58)),
                new Product(null, 15, 30, ProductType.SEASONAL, "Grapes", null,
                        today.plusDays(180), today.plusDays(240)));
        productRepository.saveAll(products);
        Order order = orderRepository.save(new Order(null, new HashSet<>(products)));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().intValue()));

        List<Integer> expectedStock = List.of(29, 0, 29, 0, 29, 30);
        for (int index = 0; index < products.size(); index++) {
            Product storedProduct = productRepository.findById(products.get(index).getId()).orElseThrow();
            assertEquals(expectedStock.get(index), storedProduct.getAvailable(), storedProduct.getName());
        }
        verify(notificationService).sendDelayNotification(10, "USB Dongle");
        verify(notificationService).sendExpirationNotification("Milk", today.minusDays(2));
        verify(notificationService).sendOutOfStockNotification("Grapes");
        verifyNoMoreInteractions(notificationService);
    }
}
