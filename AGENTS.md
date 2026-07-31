# Repository Guidelines

## Project Overview

이 저장소는 한국투자증권 Open API를 이용한 자동매매, 주가·기업정보 수집, 변동성 돌파 및 리밸런싱 백테스트를 제공하는 Kotlin/Spring Boot 애플리케이션이다. Java 17, Spring Boot 2.6.2, Gradle 7.3.2, JPA/QueryDSL, H2를 사용한다. 주문·스케줄·외부 메시지 전송 코드는 실제 계좌나 외부 서비스에 영향을 줄 수 있으므로 변경 및 실행 범위를 먼저 확인한다.

## Project Structure

- `src/main/kotlin/com/setvect/bokslstock2`: 애플리케이션 루트 패키지. 새 코드는 이 패키지 체계를 유지한다.
  - `backtest`: 공통 계좌·리포트 로직과 변동성 돌파(`vbs`), 리밸런싱(`rebalance`) 백테스트.
  - `crawl`: Finviz, Naver, 주가 및 국내 기업정보 수집기.
  - `index`: 종목·캔들 엔티티, 저장소, 이동평균과 CSV 저장 로직.
  - `koreainvestment`: REST 주문/조회, WebSocket 시세, 실전 변동성 돌파 스케줄.
  - `strategy/companyvalue`: 한국·미국 기업 가치평가 전략.
  - `config`, `controller`, `slack`, `util`: 공통 설정, HTTP 진입점, 알림, 유틸리티.
- `src/main/resources`: `application.yml`, Logback 설정, 정적 HTML, PTP 제외 종목 자료.
- `src/test/kotlin`: 기본 빌드에 포함되는 결정적인 JUnit 5 테스트.
- `src/testDependency/kotlin`: 외부 API, 로컬 DB·파일, 네트워크 또는 수동 분석에 의존하는 별도 소스셋. 기본 `build`에는 포함되지 않는다.
- `http/`, `api/`: IntelliJ HTTP Client용 REST 호출 예시. 엔드포인트 변경 시 함께 갱신한다.
- `docs/`: 테이블 구조, 메모, 용어와 작업 기록. DB 구조 변경 시 `docs/TABLE_SCHEMA.md`도 확인한다.
- `script/`: 설치 번들에 포함되는 Linux 실행 스크립트.
- `db/`, `logs/`, `crawl/`, `backtest-result/`, `data_source/`: 로컬 실행 데이터 또는 생성 결과이며 Git에 추가하지 않는다.

`build/`, `out/` 및 KAPT/QueryDSL 생성 코드는 빌드 산출물이므로 직접 수정하거나 커밋하지 않는다.

## Build and Development Commands

현재 `gradlew`에는 실행 비트가 없으므로 아래처럼 `bash ./gradlew`로 실행한다. 실행 비트를 별도 변경한 환경에서는 `./gradlew`도 가능하다.

- `bash ./gradlew clean`: 생성된 빌드 산출물을 정리한다.
- `bash ./gradlew test`: `src/test/kotlin`의 기본 테스트만 실행한다.
- `bash ./gradlew build`: 컴파일, 기본 테스트, 패키징을 수행하는 일반 검증 명령이다.
- `bash ./gradlew bootRun --args='--spring.profiles.active=local'`: 로컬 프로필로 애플리케이션을 실행한다.
- `bash ./gradlew testDependency --tests 'fully.qualified.TestClass'`: 환경 의존 테스트를 필요한 클래스만 선택해 실행한다.
- `bash ./gradlew makeInstallFile`: JAR, 설정, 스크립트를 `build/generated/source/kapt/main/dist` 아래에 구성한다. `build.gradle.kts`에서 `buildDir`를 재지정하므로 일반적인 `build/dist`가 아니다.

`deployRemote`는 원격 업로드와 재시작을 수행한다. 명시적인 배포 요청, 대상 서버 확인, `authFile`, `remoteUser`, `remoteHost`, `remotePort`, `remoteDir`, `restartCommand` Gradle 속성이 모두 준비된 경우에만 실행한다.

## Coding and Architecture Conventions

- Kotlin 표준 4칸 들여쓰기, `PascalCase` 타입명, `camelCase` 함수·프로퍼티, 소문자 패키지를 따른다. 자동 포매터나 린터가 없으므로 수정 파일의 인접 코드 스타일을 따른다.
- Spring 컴포넌트는 기존 코드처럼 주 생성자 주입을 우선하고, DTO·요청·응답·값 객체에는 용도에 맞게 `data class`를 사용한다.
- 계층은 대체로 `controller -> service -> repository/entity` 방향을 유지한다. 공통 계산은 `backtest/common` 또는 `util`에 두되, 특정 도메인 로직을 무리하게 공통화하지 않는다.
- 설정 바인딩 prefix는 현재 `com.setvect.bokslstock`이다. 루트 패키지명 `bokslstock2`와 다르므로 설정 키를 임의로 바꾸지 말고 `BokslStockProperties`와 `application.yml`을 함께 수정한다.
- 한국투자증권 요청/응답 및 WebSocket 모델의 필드명, 거래 ID, 숫자 단위는 외부 API 계약이다. 이름 정리나 타입 변경을 할 때 직렬화 결과와 호출 예시를 반드시 확인한다.
- `@Scheduled`, 주문, 토큰 발급, WebSocket 이벤트 로직을 수정할 때는 중복 주문, 장 운영시간, 재시도, 토큰 만료, 비동기 실행의 영향을 검토한다.
- 기존 공개 이름의 오탈자(`Serivce`, `Trad...` 등)는 관련 참조를 모두 안전하게 변경하는 작업이 아니라면 주변 수정과 함께 정리하지 않는다.

## Testing Guidelines

- JUnit 5와 `spring-boot-starter-test`를 사용한다. 일반 테스트 클래스는 `SomethingTest.kt`로 명명하고 기본적으로 `src/test/kotlin`에 둔다.
- 서비스 계산, 날짜·수익률 로직, 직렬화처럼 외부 환경 없이 검증 가능한 동작에는 작은 단위 테스트를 추가한다. Spring 컨텍스트가 필요할 때만 `@SpringBootTest`를 사용한다.
- 프로필이 필요한 Spring 테스트는 기존 관례대로 `@ActiveProfiles("test")`를 명시한다. 테스트용 `application.yml`은 Git에서 제외되어 있으므로 로컬 파일 존재를 가정하지 않는다.
- `testDependency`에는 크롤링, 실계좌 API, Slack 전송, 로컬 H2 데이터, 엑셀/리포트 생성처럼 부작용이 있는 테스트가 포함되어 있다. 전체 태스크를 관성적으로 실행하지 말고 대상 테스트의 코드와 설정을 먼저 확인한 뒤 클래스 단위로 실행한다.
- 문서만 수정한 경우에는 diff와 경로·명령의 정확성을 확인한다. Kotlin/Gradle 변경은 최소 `bash ./gradlew test`, 통합 흐름이나 패키징 변경은 `bash ./gradlew build`까지 수행한다. 환경 의존 실패는 기본 테스트 실패와 구분해 보고한다.

## Configuration, Data, and Security

- 로컬 실행에는 Java 17과 `local` 프로필이 필요하다. `APPKEY`, `APPSECRET`, `ACCOUNTNO`는 환경 변수로 전달하며 실제 계좌번호나 키를 코드, 문서, HTTP 예시, 로그에 넣지 않는다.
- Slack `token`과 `channelId`, 원격 배포 인증 파일 및 Gradle 속성도 비밀정보로 취급한다. 커밋 전 `application.yml`, 로컬 테스트 설정, HTTP private environment와 diff를 확인한다.
- 기본 DB는 `jdbc:h2:file:./db/bokslstock_db` 형태의 로컬 파일 DB다. 스키마/엔티티 변경 시 기존 데이터 호환성과 QueryDSL 생성 여부를 확인하고 재현 절차 또는 테이블 문서를 함께 남긴다.
- 외부 API를 호출하는 코드에는 민감한 헤더·응답 전문을 로그로 남기지 않는다. 수집 결과, 백테스트 리포트, PID와 실행 로그는 소스가 아닌 로컬 산출물로 유지한다.

## Commit and Pull Request Guidelines

- 작업 전후 `git status --short`와 `git diff`로 범위를 확인하고, 사용자 변경이나 무관한 생성 파일을 덮어쓰지 않는다.
- 커밋은 한 가지 목적에 집중하고 기존 이력처럼 짧은 한국어 제목을 사용한다(예: `크롤러 지연 처리`).
- PR에는 변경 요약, 영향받는 주요 파일/도메인, 실행한 테스트와 실행하지 못한 환경 의존 검증을 적는다. API 또는 DB 계약 변경은 호환성 영향과 샘플/문서 갱신 여부를 포함한다.
- UI 또는 생성 리포트의 시각적 결과가 달라지면 스크린샷이나 결과 예시를 첨부한다.
