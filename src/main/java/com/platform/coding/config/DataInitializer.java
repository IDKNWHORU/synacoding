package com.platform.coding.config;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션 시작 시 초기 데이터를 설정하는 클래스입니다.
 * application.yml에 정의된 관리자 계정 정보를 읽어 DB에 저장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // application.yml의 값을 주입받습니다.
    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // DB에 해당 이메일의 관리자가 이미 존재하는지 확인합니다.
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("관리자 계정 '{}'이(가) 이미 존재하므로 초기화 과정을 건너뜁니다.", adminEmail);
        } else {
            // 존재하지 않으면 새로운 관리자 계정을 생성합니다.
            log.info("관리자 계정 '{}'을(를) 생성합니다.", adminEmail);
            User admin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword)) // 비밀번호를 암호화합니다.
                    .userName(adminUsername)
                    .userType(UserType.SUPER_ADMIN)
                    .isActive(true)
                    .build();

            userRepository.save(admin);
            log.info("관리자 계정 생성이 완료되었습니다.");
        }
    }
}