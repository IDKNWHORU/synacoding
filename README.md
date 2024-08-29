# Ludium World BackEnd

**Ludium World BackEnd**은 Spring Boot 3.1.x와 Java 17을 기반으로 구축된 백엔드 애플리케이션입니다. 이 프로젝트는 PostgreSQL 데이터베이스와 연동되며, Google OAuth2 인증을 지원합니다. 웹 애플리케이션 개발을 위한 다양한 Spring Boot 모듈을 활용하여 확장 가능하고 유지보수가 용이한 구조를 갖추고 있습니다.

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [설치 및 실행](#설치-및-실행)
- [프로젝트 구조](#프로젝트-구조)
- [빌드 및 배포](#빌드-및-배포)
- [사용된 기술](#사용된-기술)
- [환경 설정](#환경-설정)
- [기여 방법](#기여-방법)
- [라이선스](#라이선스)

## 프로젝트 소개

이 프로젝트는 RESTful API 제공, 데이터베이스와의 상호작용, 비동기 웹 서비스 지원 및 OAuth2 인증 기능을 포함하는 백엔드 애플리케이션입니다. 주로 Spring Boot 프레임워크를 사용하여 개발되었으며, Google OAuth2 인증과 PostgreSQL 데이터베이스를 통해 강력한 사용자 인증 및 데이터 관리 기능을 제공합니다.

## 설치 및 실행

### 필수 요구 사항

- Java 17 이상
- Gradle 7.5 이상
- PostgreSQL 데이터베이스

### 설치 방법

1. **레포지토리 클론:**

   ```bash
   git clone https://github.com/Ludium-Official/ludium-world-back-end.git
   cd ludium-world-back-end
   ```

2. **의존성 설치:**

   Gradle을 사용하여 프로젝트의 의존성을 설치합니다.

   ```bash
   ./gradlew build
   ```

3. **환경 변수 설정:**

   `application.properties` 파일 또는 YAML 형식으로 설정 파일을 생성하여 PostgreSQL 데이터베이스와 Google OAuth2 인증을 구성합니다.

4. **애플리케이션 실행:**

   ```bash
   ./gradlew bootRun
   ```

## 프로젝트 구조

- **`config/`**: 애플리케이션 설정 파일이 위치한 디렉터리.
- **`gradle/`**: Gradle 빌드 도구 관련 설정 파일.
- **`src/main/java/`**: 주요 애플리케이션 코드.
- **`src/main/resources/`**: 애플리케이션 설정 파일 및 정적 자원.
- **`src/test/java/`**: 테스트 코드.

## 빌드 및 배포

1. **빌드:**

   ```bash
   ./gradlew build
   ```

   빌드 후, `build/libs/` 디렉터리에 `.jar` 파일이 생성됩니다.

2. **배포:**

   생성된 `.jar` 파일을 원하는 서버에 배포하여 실행할 수 있습니다.

   ```bash
   java -jar build/libs/프로젝트이름-버전.jar
   ```

## 사용된 기술

- **Java 17 이상**: 백엔드 애플리케이션 개발 언어.
- **Spring Boot 3.1.x**: 웹 애플리케이션 및 마이크로서비스 개발을 위한 프레임워크.
  - **Spring Boot Starter Web**: RESTful 웹 애플리케이션 개발을 위한 모듈.
  - **Spring Boot Starter WebFlux**: 비동기 및 논블로킹 웹 애플리케이션 개발을 위한 모듈.
  - **Spring Boot Starter Data JPA**: JPA를 사용한 데이터베이스 접근을 지원.
- **PostgreSQL**: 데이터베이스 관리 시스템.
- **Gradle 7.5 이상**: 빌드 자동화 도구.
- **Checkstyle**: 코드 스타일 검사 도구.
- **JUnit 5**: 테스트 프레임워크.

## 환경 설정

프로젝트에서 사용되는 주요 설정은 다음과 같습니다. 이 설정을 `application.properties` 또는 `application.yml` 파일에 추가하세요:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret
            provider:
              redirect-uri: your-google-redirect-uri
        provider:
          google:
            authorization-uri: https://accounts.google.com/o/oauth2/v2/auth
            token-uri: https://oauth2.googleapis.com/token
            resource-uri: https://www.googleapis.com/oauth2/v2/userinfo
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://your-database-url/your-database-name
    username: your-database-username
    password: your-database-password

logging:
  level:
    root: INFO
    org.springframework.security: DEBUG
```

## 기여 방법

기여는 언제나 환영합니다! 기여하려면 다음 단계를 따라 주세요:

1. 이 레포지토리를 포크합니다.
2. 새로운 기능 브랜치를 생성합니다 (`git checkout -b feature/YourFeature`).
3. 변경 사항을 커밋합니다 (`git commit -m 'Add some feature'`).
4. 브랜치에 푸시합니다 (`git push origin feature/YourFeature`).
5. 풀 리퀘스트를 엽니다.

## 라이선스

이 프로젝트는 MIT 라이선스에 따라 제공됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

---

이 내용을 기반으로 프로젝트에 맞게 `README.md` 파일을 수정하고 배포하시면 됩니다. 추가적인 세부사항이 필요하거나 수정할 부분이 있다면 알려주세요!
