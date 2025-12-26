# TODO: 한국투자증권(KIS) API 기반 가치평가 데이터 수집

목표: 기존 크롤링을 KIS Open API로 대체하되 `ValueAnalysisKoreanCompanyService.kt`가 사용하는 데이터 모델을 변경하지 않는다.

## 0) 수집 가능 여부 확인 (가장 먼저)
- [ ] 작업(Codex): `docs/api/[국내주식] 기본시세.xlsx`, `docs/api/[국내주식] 시세분석.xlsx`, `docs/api/[국내주식] 종목정보.xlsx`를 읽고 필요한 필드를 API 엔드포인트/필드로 매핑한다. 누락/제한 사항(예: 과거 데이터)을 정리한다.
- [ ] 산출물: 필드 -> KIS 엔드포인트/필드, 비고를 담은 간단한 매핑 표와 갭 목록.
- [ ] 확인(사용자): `KoreanCompanyDetail`에 필요한 필드가 모두 커버되었는지, 불가 항목이 명확히 표시되었는지 확인.

## 1) 목표 데이터 계약 정의 (서비스 수정 없음)
- [ ] 작업(Codex): `KoreanCompanyDetail` 및 중첩 타입과 동일한 JSON 스키마를 문서화한다. 필드명/타입을 그대로 유지한다.
- [ ] 산출물: 스키마 개요 + 예시 JSON 문서.
- [ ] 확인(사용자): `src/main/kotlin/com/setvect/bokslstock2/strategy/companyvalue/model/KoreanCompanyDetail.kt`와 일치하는지 확인.

## 2) 선택 필드/기본값 규칙 결정
- [ ] 작업(Codex): KIS에서 제공하지 않는 경우 null 허용 여부와 기본 처리 규칙을 정의한다(예: `historyData`, `dvr`, `per`, `pbr`).
- [ ] 산출물: 필드 -> 규칙 간단 테이블.
- [ ] 확인(사용자): `ValueAnalysisKoreanCompanyService.kt`의 필터링 로직과 충돌 없는지 확인.

## 3) KIS 종목정보 API 클라이언트 구축
- [ ] 작업(Codex): 기존 `StockClientService.kt`와 유사한 구조로 종목정보 전용 서비스(거래 기능과 분리)를 추가한다.
- [ ] 산출물: 신규 클라이언트 클래스 + 요청/응답 DTO (예: `koreainvestment/info` 패키지).
- [ ] 확인(사용자): 헤더, TR ID, 엔드포인트 경로가 엑셀 문서와 일치하는지 확인.

## 4) `KoreanCompanyDetail` 리스트 수집기 구현
- [ ] 작업(Codex): 수집기 서비스 구현:
  - [ ] 코스피 + 코스닥 상장 종목 목록 수집
  - [ ] 업종, 발행주식수, 현재가, PER/PBR/배당수익률 등 보강
  - [ ] `CrawlerKoreanCompanyProperties.kt`의 파일명 규칙대로 `./crawl/stock.naver.com/detail-list.json` 저장
- [ ] 산출물: 수집기 서비스 + 실행 엔트리(커맨드 또는 테스트)
- [ ] 확인(사용자): 생성된 JSON의 샘플 몇 개를 열어 값이 기대와 맞는지 확인

## 5) `ValueAnalysisKoreanCompanyService.kt` 수정 없음 보장
- [ ] 작업(Codex): 신규 수집기의 출력이 기존 서비스에서 그대로 로딩되도록 맞춘다.
- [ ] 산출물: `src/main/kotlin/com/setvect/bokslstock2/strategy/companyvalue/service/ValueAnalysisKoreanCompanyService.kt` 무수정.
- [ ] 확인(사용자): 기존 분석 플로우에서 파싱 오류 없이 동작하는지 확인.

## 6) (선택) 과거 데이터 보강
- [ ] 작업(Codex): KIS에서 동등한 과거 데이터가 제공될 때만 적용. 제공되지 않으면 생략.
- [ ] 산출물: 보강 로직 또는 생략 사유 기록.
- [ ] 확인(사용자): `historyData`가 채워졌거나 의도적으로 비어있는지 확인.

## 7) 최소 테스트 추가
- [ ] 작업(Codex): 매핑/직렬화 로직에 대한 최소 테스트 추가(필요 시 `testDependency` 사용).
- [ ] 산출물: `src/testDependency/kotlin` 아래 테스트 추가.
- [ ] 확인(사용자): `./gradlew testDependency` 실행.
