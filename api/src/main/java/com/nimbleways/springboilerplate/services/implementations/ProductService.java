package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public void processProduct(Product product) {
        log.debug("Processing product {} of type {}", product.getId(), product.getType());
        switch (product.getType()) {
            case NORMAL -> processNormalProduct(product);
            case SEASONAL -> processSeasonalProduct(product);
            case EXPIRABLE -> processExpirableProduct(product);
        }
    }

    private void processNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            decrementStock(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product);
        }
    }

    private void processSeasonalProduct(Product product) {
        if (LocalDate.now().isAfter(product.getSeasonStartDate())
                && LocalDate.now().isBefore(product.getSeasonEndDate())
                && product.getAvailable() > 0) {
            decrementStock(product);
        } else if (LocalDate.now().plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
            log.debug("Product {} unavailable: replenishment would arrive after season end {}",
                    product.getId(), product.getSeasonEndDate());
            notificationService.sendOutOfStockNotification(product.getName());
            product.setAvailable(0);
            productRepository.save(product);
        } else if (product.getSeasonStartDate().isAfter(LocalDate.now())) {
            log.debug("Product {} unavailable: season starts on {}", product.getId(), product.getSeasonStartDate());
            notificationService.sendOutOfStockNotification(product.getName());
            productRepository.save(product);
        } else {
            notifyDelay(product);
        }
    }

    private void processExpirableProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            decrementStock(product);
        } else {
            log.debug("Expirable product {} unavailable: stock={}, expiryDate={}",
                    product.getId(), product.getAvailable(), product.getExpiryDate());
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
        log.debug("Decremented stock for product {} to {}", product.getId(), product.getAvailable());
    }

    private void notifyDelay(Product product) {
        int leadTime = product.getLeadTime();
        productRepository.save(product);
        log.debug("Sending delay notification for product {}: leadTime={} days", product.getId(), leadTime);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }
}
