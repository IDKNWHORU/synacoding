package com.platform.coding.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByParent(User parent);

    /**
     * 특정 기간 동안 가입한 신규 사용자 수를 계산한다.
     */
    long countByCreatedAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 이메일(대소문자 무시)로 사용자를 검색하고 페이지네이션하여 반환한다.
     */
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * 사용자 이름(대소문자 무시)으로 사용자를 검색하고 페이지네이션하여 반환한다.
     */
    Page<User> findByUserNameContainingIgnoreCase(String userName, Pageable pageable);
}
