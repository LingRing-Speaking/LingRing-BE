# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 명령어

시스템 Gradle 대신 Gradle Wrapper(`./gradlew`)를 사용합니다.

- 앱 실행: `./gradlew bootRun`
- 빌드(jar): `./gradlew build` (결과물은 `build/libs/`)
- 전체 테스트 실행: `./gradlew test`
- 특정 테스트 클래스 실행: `./gradlew test --tests com.lingring.LingRingApplicationTests`
- 특정 테스트 메서드 실행: `./gradlew test --tests 'com.lingring.LingRingApplicationTests.contextLoads'`

## 스택 / 버전

- **Spring Boot 4.0.5** + **Java 25** (toolchain은 `build.gradle`에 고정). LTS 기준보다 앞서 있으므로 Boot 3.x / Java 17–21 전용 API·의존성을 제안하지 말 것. 웹 스타터는 기존 `spring-boot-starter-web`이 아니라 신규 모듈형 `spring-boot-starter-webmvc`이며, 테스트 스타터도 `spring-boot-starter-webmvc-test`를 사용한다.
- JPA + MySQL (`mysql-connector-j` 런타임). 현재 `src/main/resources/application.yml`에 datasource 설정이 없어서, 실제 DB 연결 설정을 추가하기 전에는 앱이 기동되지 않는다.
- Lombok은 `main` / `test` 양쪽 모두에 compileOnly + annotationProcessor로 설정되어 있다.

## 구조상 주의점

- 루트 패키지: `com.lingring` (메인 클래스 `LingRingApplication`).
- `settings.gradle`의 루트 프로젝트 이름은 `backend`로 저장소명 `LingRing-BE`와 다르다. Gradle 프로젝트명은 `backend`이며 jar 산출물 이름, IDE 프로젝트 표시명에 영향을 준다.
- `.gitignore`가 `src/main/resources/` 및 `src/test/resources/` 아래의 **모든 `*.yml`을 제외**한다. 이미 추적 중인 파일만 예외이므로, 프로파일별 설정(`application-dev.yml` 등)을 추가해도 git이 조용히 무시한다. 의도적으로 커밋하려면 `git add -f`를 쓰거나 무시되지 않는 경로로 옮길 것.
- `HELP.md`도 gitignore 대상(Spring Initializr 기본 산출물).

## 이슈 / PR 컨벤션

- 이슈 템플릿은 `.github/ISSUE_TEMPLATE/`에 있음 (`bug-template.md`, `task-template.md`). 이슈를 열 때 이 구조를 따른다.
- 최근 커밋 메시지는 한국어 Conventional Commits 스타일 (예: `docs: pr template 추가`, `chore: init project`). 동일한 스타일을 유지할 것.