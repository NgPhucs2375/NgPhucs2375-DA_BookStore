package com.example.bookstore.repository;

import com.example.bookstore.model.Voucher;
import com.example.bookstore.model.User;
import com.example.bookstore.model.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    List<Voucher> findBySeller(User seller);
    Optional<Voucher> findByCode(String code);
    List<Voucher> findBySellerAndStatus(User seller, VoucherStatus status);
    List<Voucher> findBySellerAndNameContainingIgnoreCase(User seller, String name);
}
