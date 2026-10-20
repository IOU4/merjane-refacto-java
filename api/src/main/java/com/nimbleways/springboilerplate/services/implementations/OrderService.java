package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    @Transactional
    public Long processOrder(Long orderId) {
        log.info("Processing order {}", orderId);
        Order order = orderRepository.findById(orderId).orElseThrow();
        for (Product product : order.getItems()) {
            productService.processProduct(product);
        }
        log.debug("Processed all items for order {}", order.getId());
        return order.getId();
    }
}
