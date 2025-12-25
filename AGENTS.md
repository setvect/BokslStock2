# Repository Guidelines

## Project Structure & Module Organization
`src/main/kotlin`에는 Spring Boot 컨트롤러, 서비스, 설정이 모여 있으며 패키지 이름은 `com.setvect.bokslstock2` 형태를 유지한다. 테스트 코드는 `src/test/kotlin`과 확장 테스트인 `src/testDependency/kotlin`으로 분리되고, 정적 리소스·설정은 `src/main/resources`에 둔다. REST 샘플(`docs/http`, `docs/api`), 설치 스크립트(`script/`), 로컬 H2 DB(`db/`) 위치를 헷갈리지 말 것.

## Build, Test, and Development Commands
- `./gradlew clean`: Gradle 캐시와 산출물을 모두 정리한다.
- `./gradlew build`: 전체 빌드와 기본 JUnit 테스트를 수행해 배포 가능 상태를 확인한다.
- `./gradlew test`: `src/test/kotlin` 하위 단위/통합 테스트만 실행할 때 사용한다.
- `./gradlew testDependency`: 커스텀 확장 테스트 세트를 돌릴 때 사용한다.
- `./gradlew bootRun`: 로컬에서 Spring Boot 애플리케이션을 즉시 실행한다.
- `./gradlew makeInstallFile`: `build/dist`에 JAR·설정·스크립트가 포함된 배포 번들을 만든다.

## Coding Style & Naming Conventions
Kotlin 표준(4-space indent, `PascalCase` 클래스, `camelCase` 함수/변수, 소문자 패키지)을 따른다. crawl/backtest/koreainvestment 모듈과 naming을 최대한 일관되게 유지하며 자동 포매터·린터가 없으니 인접 코드와 동일한 스타일을 수동으로 맞춘다.

## Testing Guidelines
JUnit 5(`spring-boot-starter-test`)을 기본으로 사용한다. 테스트 파일은 `SomethingTest.kt`처럼 명명하고, 필요 시 `testDependency`에만 의존하는 케이스는 해당 디렉터리에 배치한다. 커버리지가 명시되지는 않았지만 주요 서비스 레이어와 재사용 로직에는 단위 테스트를 추가하고, 새로운 통합 흐름이 있으면 `bootRun` 이전에 `./gradlew build`로 검증한다.

## Commit & Pull Request Guidelines
커밋 메시지는 짧고 한국어로 작성하며 단일 목적을 강조한다(예: "크롤러 지연 처리"). PR 설명에는 변경 요약, 핵심 파일, 테스트 여부를 포함하고 UI 변화가 있다면 스크린샷을 첨부한다. 로컬 실행 시 `APPKEY`, `APPSECRET`, `ACCOUNTNO` 환경 변수를 설정하고 민감한 키를 저장소에 절대 커밋하지 않는다.

## Security & Configuration Tips
`src/main/resources/application.yml`에서 FRED·DART 키를 관리하되 더미 값만 커밋하고, 실제 값은 환경 변수 또는 외부 시크릿 매니저에 둔다. 로컬 H2 파일 DB(`jdbc:h2:file:./db/bokslstock_db`)를 사용하므로 새 스키마 변경은 마이그레이션 스크립트나 문서(`docs/`)에 기록해 다른 기여자가 재현할 수 있게 한다.
