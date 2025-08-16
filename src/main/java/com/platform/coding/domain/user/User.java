package com.platform.coding.domain.user;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = @Index(name = "idx_user_email", columnList = "email"), schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_id")
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    // 부모-자식 관계 (자기 자신을 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private User parent;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "parent")
    private List<User> children = new ArrayList<>();

    @Column(name = "refresh_token", length = 512, unique = true)
    private String refreshToken;

    @Builder
    public User(String email, String passwordHash, String userName, UserType userType, User parent, boolean isActive) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userName = userName;
        this.userType = userType;
        this.parent = parent;
        this.isActive = isActive;
    }

    // 연관관계 편의 메소드
    public void addChild(User child) {
        this.children.add(child);
        child.setParent(this);
    }

    void setParent(User parent) {
        this.parent = parent;
    }

    /**
     * 비밀번호를 업데이트합니다.
     * @param newPasswordHash 새로 암호화된 비밀번호
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * 리프레시 토큰을 업데이트합니다.
     * @param refreshToken 새로운 리프레시 토큰 (로그아웃 시 null)
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 관리자가 회원의 활성 상태를 변경함.
     * @param isActive 새로운 활성 상태
     */
    public void updateActiveStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
