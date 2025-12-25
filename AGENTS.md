# Repository Guidelines

## 기본사항
- 모든 답변은 한국어로 해줘. 기술용어를 포함한 전문용어, 고유명사 등은 영어로 해도 돼. 

## Project Structure & Module Organization
- `src/main/kotlin`: Kotlin Spring Boot application code (controllers, services, configs, etc.).
- `src/main/resources`: runtime config and assets (`application.yml`, `logback-spring.xml`, `static/`, `assets/`).
- `src/test/kotlin`: primary unit/integration tests (JUnit 5).
- `src/testDependency/kotlin`: additional test suite used by the custom `testDependency` task.
- `docs/`: project notes and schemas; `http/` and `api/` contain REST call examples.
- `script/`: packaging scripts copied into the install bundle.
- `db/`: local H2 file database (`jdbc:h2:file:./db/bokslstock_db`).

## Build, Test, and Development Commands
- `./gradlew clean`: removes build outputs.
- `./gradlew build`: compiles and runs the standard test suite.
- `./gradlew test`: runs JUnit tests in `src/test/kotlin`.
- `./gradlew testDependency`: runs the extended test suite in `src/testDependency/kotlin`.
- `./gradlew bootRun`: starts the Spring Boot app locally.
- `./gradlew makeInstallFile`: creates the distribution in `build/dist` (jar + config + scripts).

## Coding Style & Naming Conventions
- Kotlin code follows standard conventions: 4-space indent, `PascalCase` for classes, `camelCase` for functions/vars, and lowercase packages (e.g., `com.setvect.bokslstock2`).
- Keep naming aligned with existing modules such as `crawl`, `backtest`, and `koreainvestment`.
- No formatter/linter is configured in Gradle; keep edits consistent with nearby code.

## Testing Guidelines
- Test framework: JUnit 5 via `spring-boot-starter-test`.
- Add tests under `src/test/kotlin` unless they depend on the extended `testDependency` suite.
- Naming: `*Test.kt` (example: `CrawlerDartServiceTest.kt`).

## Commit & Pull Request Guidelines
- Commit history uses short, descriptive messages (mostly Korean) without conventional prefixes.
- Prefer concise, single-purpose commits (e.g., ����� ���� ������).
- No PR template found; include: a summary, key files touched, and how you tested. Add screenshots only for UI changes.

## Configuration & Secrets
- Local profile uses `src/main/resources/application.yml` and expects `APPKEY`, `APPSECRET`, and `ACCOUNTNO` env vars.
- API keys for FRED/DART are configured in `application.yml`; do not commit real keys.
