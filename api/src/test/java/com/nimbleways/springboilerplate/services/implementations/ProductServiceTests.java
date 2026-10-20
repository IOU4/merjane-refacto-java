package com.nimbleways.springboilerplate.services.implementations;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, notificationService);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "available stock,                 2, 15,  1, true,  false",
            "last unit with zero lead time,   1,  0,  0, true,  false",
            "available with negative lead,    2, -1,  1, true,  false",
            "empty with positive lead time,   0, 15,  0, true,  true",
            "empty with zero lead time,       0,  0,  0, false, false",
            "empty with negative lead time,   0, -1,  0, false, false",
            "negative stock with delay,      -2, 15, -2, true,  true",
            "negative stock without delay,   -2,  0, -2, false, false"
    })
    void processesNormalProduct(String scenario, int available, int leadTime, int expectedStock,
                               boolean shouldSave, boolean shouldNotifyDelay) {
        Product product = new Product(null, leadTime, available, ProductType.NORMAL,
                scenario, null, null, null);

        productService.processProduct(product);

        assertEquals(expectedStock, product.getAvailable());
        assertEquals(leadTime, product.getLeadTime());
        if (shouldSave) {
            verify(productRepository).save(product);
        } else {
            verifyNoInteractions(productRepository);
        }
        if (shouldNotifyDelay) {
            verify(notificationService).sendDelayNotification(leadTime, scenario);
        } else {
            verifyNoInteractions(notificationService);
        }
        verifyNoMoreInteractions(productRepository, notificationService);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "in season with stock despite long lead, -1,   1, 15,  2,  1, NONE",
            "empty and replenishment within season,  -1,  30, 15,  0,  0, DELAY",
            "replenishment after season end,          -1,  10, 15,  0,  0, OUT_OF_STOCK",
            "replenishment exactly on season end,     -1,  15, 15,  0,  0, DELAY",
            "replenishment before season end,         -1,  16, 15,  0,  0, DELAY",
            "before season with stock,                 5,  30, 15,  2,  2, OUT_OF_STOCK",
            "before season without stock,              5,  30, 15,  0,  0, OUT_OF_STOCK",
            "before season but replenishment too late, 5,  10, 15,  2,  0, OUT_OF_STOCK",
            "on season start with stock,               0,  30, 15,  2,  2, DELAY",
            "on season start without stock,            0,  30, 15,  0,  0, DELAY",
            "on season end with positive lead,        -1,   0, 15,  2,  0, OUT_OF_STOCK",
            "on season end with zero lead,            -1,   0,  0,  2,  2, DELAY",
            "after season with stock,                -30,  -1,  0,  2,  0, OUT_OF_STOCK",
            "in season empty with zero lead,          -1,  30,  0,  0,  0, DELAY",
            "in season empty with negative lead,      -1,  30, -3,  0,  0, DELAY",
            "in season with negative stock,           -1,  30, 15, -2, -2, DELAY"
    })
    void processesSeasonalProduct(String scenario, int seasonStartOffset, int seasonEndOffset,
                                 int leadTime, int available, int expectedStock,
                                 SeasonalNotification expectedNotification) {
        LocalDate today = LocalDate.now();
        Product product = new Product(null, leadTime, available, ProductType.SEASONAL, scenario, null,
                today.plusDays(seasonStartOffset), today.plusDays(seasonEndOffset));

        productService.processProduct(product);

        assertEquals(expectedStock, product.getAvailable());
        assertEquals(leadTime, product.getLeadTime());
        InOrder interactions = inOrder(productRepository, notificationService);
        switch (expectedNotification) {
            case NONE -> {
                interactions.verify(productRepository).save(product);
                verifyNoInteractions(notificationService);
            }
            case DELAY -> {
                interactions.verify(productRepository).save(product);
                interactions.verify(notificationService).sendDelayNotification(leadTime, scenario);
            }
            case OUT_OF_STOCK -> {
                interactions.verify(notificationService).sendOutOfStockNotification(scenario);
                interactions.verify(productRepository).save(product);
            }
        }
        verifyNoMoreInteractions(productRepository, notificationService);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "fresh with stock,            2,  1, 1, false",
            "fresh last unit,             1,  1, 0, false",
            "fresh but empty,             0,  1, 0, true",
            "fresh with negative stock,  -2,  1, 0, true",
            "expires today with stock,    2,  0, 0, true",
            "expires today but empty,     0,  0, 0, true",
            "expired with stock,          2, -1, 0, true",
            "expired but empty,           0, -1, 0, true"
    })
    void processesExpirableProduct(String scenario, int available, int expiryOffset,
                                  int expectedStock, boolean shouldNotifyExpiration) {
        LocalDate expiryDate = LocalDate.now().plusDays(expiryOffset);
        Product product = new Product(null, 15, available, ProductType.EXPIRABLE,
                scenario, expiryDate, null, null);

        productService.processProduct(product);

        assertEquals(expectedStock, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        assertEquals(expiryDate, product.getExpiryDate());
        InOrder interactions = inOrder(productRepository, notificationService);
        if (shouldNotifyExpiration) {
            interactions.verify(notificationService).sendExpirationNotification(scenario, expiryDate);
        } else {
            verifyNoInteractions(notificationService);
        }
        interactions.verify(productRepository).save(product);
        verifyNoMoreInteractions(productRepository, notificationService);
    }

    @Test
    void savesBeforeSendingNormalProductDelayNotification() {
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "USB Cable", null, null, null);

        productService.processProduct(product);

        InOrder interactions = inOrder(productRepository, notificationService);
        interactions.verify(productRepository).save(product);
        interactions.verify(notificationService).sendDelayNotification(15, "USB Cable");
        verifyNoMoreInteractions(productRepository, notificationService);
    }

    @Test
    void rejectsNullProductTypeWithoutSavingOrNotifying() {
        Product product = new Product(null, 15, 1, null, "USB Cable", null, null, null);

        assertThrows(NullPointerException.class, () -> productService.processProduct(product));

        verifyNoInteractions(productRepository, notificationService);
    }

    private enum SeasonalNotification {
        NONE,
        DELAY,
        OUT_OF_STOCK
    }
}
