package com.platform.coding.domain.refund;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    boolean existsByPaymentId(Long paymentId);
}
