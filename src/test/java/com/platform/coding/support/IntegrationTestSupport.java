package com.platform.coding.support;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers
@Disabled // 이 클래스 자체는 테스트를 실행하지 않도록 설정
public abstract class IntegrationTestSupport {
    // 재사용 가능한 PostgreSQL 컨테이너를 정의
    // 운영환경과 동일한 postgrse:17.4 이미지를 생성
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:17.4");

    // 동적으로 데이터소스 설정을 주입
    // 테스트 컨테이너가 실행된 후, 생성된 DB의 실제 접속 정보를 Spring의 Environment에 등록
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = postgreSQLContainer.getJdbcUrl();

        String urlWithSchema = jdbcUrl + "¤tSchema=platform";

        registry.add("spring.datasource.url", () -> urlWithSchema);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.datasource.hikari.connection-init-sql", () -> "CREATE SCHEMA IF NOT EXISTS platform");
    }
}
