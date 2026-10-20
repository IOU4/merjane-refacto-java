package com.nimbleways.springboilerplate.repositories;

import com.nimbleways.springboilerplate.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findFirstByName(String name);
}
