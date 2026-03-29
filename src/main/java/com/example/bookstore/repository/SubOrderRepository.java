package com.example.bookstore.repository;

import com.example.bookstore.model.SubOrder;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {
    List<SubOrder> findBySeller(User seller);

    List<SubOrder> findBySellerOrderByIdDesc(User seller);

    List<SubOrder> findBySellerAndStatus(User seller, OrderStatus status);
}
