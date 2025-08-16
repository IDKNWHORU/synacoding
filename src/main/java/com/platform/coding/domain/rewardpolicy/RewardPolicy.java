package com.platform.coding.domain.rewardpolicy;

import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "reward_policies", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RewardPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "policy_key", nullable = false, unique = true)
    private PolicyKey policyKey;

    @Column(name = "policy_value", nullable = false)
    private String policyValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    public void updateValue(String newValue, User admin) {
        this.policyValue = newValue;
        this.admin = admin;
        this.updatedAt = Instant.now();
    }
}