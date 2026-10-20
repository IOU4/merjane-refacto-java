package com.nimbleways.springboilerplate.repositories;

import com.nimbleways.springboilerplate.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
