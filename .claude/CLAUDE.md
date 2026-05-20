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
- JPA + MySQL (`mysql-connector-j` 런타임). 현재 `src/main/resources/application.yml`에 datasource 설정이 없어서, 실제 DB 연결 설정을 추가하기 전에는 앱이 기동되지 않는다.
- Lombok은 `main` / `test` 양쪽 모두에 compileOnly + annotationProcessor로 설정되어 있다.

## 구조상 주의점

- 루트 패키지: `com.lingring` (메인 클래스 `LingRingApplication`). 상세 패키지 레이아웃(도메인별 구조 포함)은 `.claude/rules/package-structure.md`를 따른다.
- `settings.gradle`의 루트 프로젝트 이름은 `backend`로 저장소명 `LingRing-BE`와 다르다. Gradle 프로젝트명은 `backend`이며 jar 산출물 이름, IDE 프로젝트 표시명에 영향을 준다.
- `HELP.md`도 gitignore 대상(Spring Initializr 기본 산출물).

## yml 파일 정책 (절대 commit 금지)

- `src/main/resources/**/*.yml`, `src/test/resources/**/*.yml` 은 **절대 git에 commit하지 않는다**. `.gitignore` 가 이 경로의 모든 `*.yml` 을 ignore하며, 과거에 잘못 추적되던 `application.yml` (main/test 양쪽) 도 본 정책 적용 시점에 추적 해제되었다.
- `git add -f` 로 강제 추가하지 말 것. `--no-verify` 와 마찬가지로 안전 장치를 우회하는 행위.
- 새 `@ConfigurationProperties` 를 도입할 때 yml 변경분을 같은 PR/커밋에 동봉하지 않는다. 도메인 정책·인프라 설정 변경 사항은 PR 본문에 텍스트로 적어 운영자/팀원에게 전달하고, 실제 yml 갱신은 각자 로컬·secret manager·배포 파이프라인 쪽에서 별도 처리한다.
- 새 yml이 필요하면 `application.yml` 외 경로(ignore되지 않는 위치)에 예시 파일을 두든지 README에 설명을 추가하는 식으로 우회. 추적되는 yml 자체를 새로 만들지 않는다.
