package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public Long processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        for (Product product : order.getItems()) {
            productService.processProduct(product);
        }
        return order.getId();
    }
}
