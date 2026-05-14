package com.example.bookstore.repository;

import com.example.bookstore.model.SubOrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubOrderStatusHistoryRepository extends JpaRepository<SubOrderStatusHistory, Long> {

    List<SubOrderStatusHistory> findBySubOrderIdOrderByCreatedAtDesc(Long subOrderId);

    List<SubOrderStatusHistory> findBySubOrderId(Long subOrderId);
}
