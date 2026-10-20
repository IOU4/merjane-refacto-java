package com.nimbleways.springboilerplate.entities;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class ProductTypeIntegrationTests {

    @Autowired
    private TestEntityManager entityManager;

    @ParameterizedTest
    @EnumSource(ProductType.class)
    void persistsTypeAsItsNameAndReadsItBack(ProductType type) {
        Product product = new Product(null, 15, 1, type, "Test product", null, null, null);
        entityManager.persistAndFlush(product);
        entityManager.clear();

        String storedType = (String) entityManager.getEntityManager()
                .createNativeQuery("SELECT type FROM products WHERE id = :id")
                .setParameter("id", product.getId())
                .getSingleResult();

        assertEquals(type.name(), storedType);
        assertEquals(type, entityManager.find(Product.class, product.getId()).getType());
    }
}
