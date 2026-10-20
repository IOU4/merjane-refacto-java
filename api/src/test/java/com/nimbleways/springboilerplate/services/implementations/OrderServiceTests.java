package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productService);
    }

    @Test
    void processesEveryProductOnceInOrderAndReturnsStoredOrderId() {
        Product firstProduct = new Product(null, 15, 1, ProductType.NORMAL, "USB Cable", null, null, null);
        Product secondProduct = new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null);
        Order order = new Order(42L, new LinkedHashSet<>(List.of(firstProduct, secondProduct)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertEquals(42L, orderService.processOrder(1L));

        InOrder interactions = inOrder(orderRepository, productService);
        interactions.verify(orderRepository).findById(1L);
        interactions.verify(productService).processProduct(firstProduct);
        interactions.verify(productService).processProduct(secondProduct);
        verifyNoMoreInteractions(orderRepository, productService);
    }

    @Test
    void returnsIdForEmptyOrderWithoutProcessingProducts() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(new Order(1L, Set.of())));

        assertEquals(1L, orderService.processOrder(1L));

        verify(orderRepository).findById(1L);
        verifyNoInteractions(productService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void preservesMissingOrderFailureWithoutProcessingProducts() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> orderService.processOrder(1L));

        verify(orderRepository).findById(1L);
        verifyNoInteractions(productService);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void stopsProcessingAndPropagatesProductFailure() {
        Product firstProduct = new Product(null, 15, 1, ProductType.NORMAL, "USB Cable", null, null, null);
        Product secondProduct = new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null);
        Order order = new Order(1L, new LinkedHashSet<>(List.of(firstProduct, secondProduct)));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        IllegalStateException failure = new IllegalStateException("Product processing failed");
        doThrow(failure).when(productService).processProduct(firstProduct);

        assertEquals(failure, assertThrows(IllegalStateException.class, () -> orderService.processOrder(1L)));

        verify(orderRepository).findById(1L);
        verify(productService).processProduct(firstProduct);
        verifyNoMoreInteractions(orderRepository, productService);
    }
}
