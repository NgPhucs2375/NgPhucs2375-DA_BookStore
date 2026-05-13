package com.example.bookstore.repository;

import com.example.bookstore.model.VoucherUsage;
import com.example.bookstore.model.User;
import com.example.bookstore.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    List<VoucherUsageRepository> findByUser(User user);
    boolean existsByUserAndVoucher(User user, Voucher voucher);
}
