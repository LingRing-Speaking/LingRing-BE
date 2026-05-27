# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 코드 작성 시 주의 사항
- YANGI 원칙을 지킬 것. (당장 필요하지 않은 기능은 미리 만들지 말라)
- DRY 원칙을 지킬 것. (동일한 코드를 반복적으로 사용하지 말라)
- API·도메인 로직 추가/수정 시 반드시 대응하는 테스트를 함께 작성한다 (상세 규칙: `.claude/rules/test-code.md`)

## 명령어

시스템 Gradle 대신 Gradle Wrapper(`./gradlew`)를 사용합니다.

- 앱 실행: `./gradlew bootRun`
- 빌드(jar): `./gradlew build` (결과물은 `build/libs/`)
- 전체 테스트 실행: `./gradlew test`
- 특정 테스트 클래스 실행: `./gradlew test --tests com.lingring.LingRingApplicationTests`
- 특정 테스트 메서드 실행: `./gradlew test --tests 'com.lingring.LingRingApplicationTests.contextLoads'`

## 스택 / 버전

- **Spring Boot 4.0.5** + **Java 25** (toolchain은 `build.gradle`에 고정). LTS 기준보다 앞서 있으므로 Boot 3.x / Java 17–21 전용 API·의존성을 제안하지 말 것. 웹 스타터는 기존 `spring-boot-starter-web`이 아니라 신규 모듈형 `spring-boot-starter-webmvc`이며, 테스트 스타터도 `spring-boot-starter-webmvc-test`를 사용한다.
- JPA + MySQL (`mysql-connector-j` 런타임).
- `spring-dotenv`로 프로젝트 루트의 `.env` 파일에서 환경변수를 자동 로드한다.
- Lombok은 `main` / `test` 양쪽 모두에 compileOnly + annotationProcessor로 설정되어 있다.

## 구조상 주의점

- 루트 패키지: `com.lingring` (메인 클래스 `LingRingApplication`). 상세 패키지 레이아웃(도메인별 구조 포함)은 `.claude/rules/package-structure.md`를 따른다.
- `settings.gradle`의 루트 프로젝트 이름은 `backend`로 저장소명 `LingRing-BE`와 다르다. Gradle 프로젝트명은 `backend`이며 jar 산출물 이름, IDE 프로젝트 표시명에 영향을 준다.
- `HELP.md`도 gitignore 대상(Spring Initializr 기본 산출물).

## yml 파일 정책

- yml 파일은 Git에 커밋한다. 민감값(비밀번호, 시크릿 키 등)은 `${ENV_VAR}` placeholder로 분리하고, 실제 값은 `.env` 파일에서 관리한다.
- `.env` 파일은 gitignore 대상이다. `.env.example`에 필요한 환경변수 키 목록을 유지한다.
- 프로필 구조: `application.yml`(기본, active profile 지정) → `application-local.yml`(로컬) / `application-dev.yml`(dev 서버). 기본 profile은 `local`이며, 서버 배포 시 `SPRING_PROFILES_ACTIVE=dev`로 오버라이드한다.
- 새 민감값을 추가할 때는 yml에 `${ENV_VAR}` placeholder를 넣고, `.env.example`에 키를 추가한다.
