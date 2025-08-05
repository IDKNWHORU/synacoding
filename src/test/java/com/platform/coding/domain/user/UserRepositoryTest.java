package com.platform.coding.domain.user;

import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class UserRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("학부모(PARENT) 사용자를 생성하고 저장한 뒤 조회할 수 있어야 한다.")
    void createAndReadParent() {
        User parentUser = User.builder()
                .email("parent@example.com")
                .passwordHash("hash_password_123")
                .userName("학부모님")
                .userType(UserType.PARENT)
                .isActive(true)
                .build();

        userRepository.save(parentUser);

        User foundUser = userRepository.findById(parentUser.getId()).orElseThrow();

        assertThat(foundUser.getId()).isNotNull();
        assertThat(foundUser.getEmail()).isEqualTo("parent@example.com");
        assertThat(foundUser.getUserName()).isEqualTo("학부모님");
        assertThat(foundUser.getUserType()).isEqualTo(UserType.PARENT);
        assertThat(foundUser.getParent()).isNull(); // 학부모이므로 parent가 없어야 함
        assertThat(foundUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("학부모에게 속한 학생(STUDENT) 사용자를 생성하고 관계를 검증한다.")
    void createStudentUserWithParent() {
        User parentUser = User.builder()
                .email("parent2@example.com")
                .passwordHash("hashed_password_456")
                .userName("보호자")
                .userType(UserType.PARENT)
                .build();
        userRepository.save(parentUser);

        User studentUser = User.builder()
                .email("student@example.com")
                .passwordHash("hashed_password_789")
                .userName("학생")
                .userType(UserType.STUDENT)
                .parent(parentUser)
                .build();

        userRepository.save(studentUser);

        User foundStudent = userRepository.findById(studentUser.getId()).orElseThrow();

        assertThat(foundStudent.getUserName()).isEqualTo("학생");
        assertThat(foundStudent.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(foundStudent.getParent()).isNotNull();
        assertThat(foundStudent.getParent().getId()).isEqualTo(parentUser.getId());
        assertThat(foundStudent.getParent().getUserName()).isEqualTo("보호자");
    }
}
