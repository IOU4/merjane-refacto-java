package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
class OrderServiceIntegrationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private NotificationService notificationService;

    @Test
    void loadsOrderItemsAndCommitsStockChangesWithoutWebRequest() {
        List<Product> products = List.of(
                new Product(null, 15, 2, ProductType.NORMAL, "USB Cable", null, null, null),
                new Product(null, 10, 3, ProductType.NORMAL, "USB Dongle", null, null, null));
        productRepository.saveAll(products);
        Order order = orderRepository.save(new Order(null, new HashSet<>(products)));

        assertEquals(order.getId(), orderService.processOrder(order.getId()));

        assertTrue(orderRepository.existsById(order.getId()));
        for (Product product : products) {
            assertTrue(productRepository.existsById(product.getId()));
            Product storedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertEquals(product.getAvailable() - 1, storedProduct.getAvailable());
        }
        verifyNoInteractions(notificationService);
    }

    @Test
    void rollsBackEarlierStockChangesWhenLaterProductProcessingFails() {
        LocalDate expiryDate = LocalDate.now().minusDays(1);
        List<Product> products = List.of(
                new Product(null, 15, 2, ProductType.EXPIRABLE, "Milk", expiryDate, null, null),
                new Product(null, 10, 3, ProductType.EXPIRABLE, "Butter", expiryDate, null, null));
        productRepository.saveAll(products);
        Order order = orderRepository.save(new Order(null, new HashSet<>(products)));
        AtomicInteger notificationCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (notificationCount.incrementAndGet() == 2) {
                throw new IllegalStateException("Notification failed");
            }
            return null;
        }).when(notificationService).sendExpirationNotification(anyString(), eq(expiryDate));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> orderService.processOrder(order.getId()));

        assertEquals("Notification failed", failure.getMessage());
        for (Product product : products) {
            Product storedProduct = productRepository.findById(product.getId()).orElseThrow();
            assertEquals(product.getAvailable(), storedProduct.getAvailable());
        }
        verify(notificationService, times(2)).sendExpirationNotification(anyString(), eq(expiryDate));
    }
}
