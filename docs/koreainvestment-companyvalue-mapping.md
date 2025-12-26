# 한국투자증권 API 수집 매핑

## 매핑 표
| 대상 필드 | KIS API (시트/엔드포인트) | 응답 필드 | 비고 |
| --- | --- | --- | --- |
| summary.code | 주식기본조회 `/uapi/domestic-stock/v1/quotations/search-stock-info` | `pdno` (요청 `PDNO`의 6자리 종목코드 사용 권장) | 응답 `pdno`는 12자리로 보임. 입력 종목코드를 유지하는 편이 안전. |
| summary.name | 주식기본조회 | `prdt_abrv_name` 또는 `prdt_name` | 예시: `prdt_abrv_name`가 한글 약칭. |
| summary.market | 주식기본조회 | `mket_id_cd` 또는 `excg_dvsn_cd` | `excg_dvsn_cd` 02/03을 KOSPI/KOSDAQ으로 매핑 필요. |
| summary.currentPrice | 주식현재가 시세 `/uapi/domestic-stock/v1/quotations/inquire-price` | `stck_prpr` | 현재가 직접 제공. |
| summary.capitalization | 주식현재가 시세 | `hts_avls` | 시가총액 직접 제공. 단위(원/억원) 확인 필요. |
| normalStock | 주식기본조회 | `stck_kind_cd`, `scty_grp_id_cd`, `etf_dvsn_cd` | `stck_kind_cd=101`을 보통주로 판단하는 로직 필요. |
| industry | 주식현재가 시세 또는 주식기본조회 | `bstp_kor_isnm` 또는 `std_idst_clsf_cd_name` | 업종 표기 차이 가능성 있음. |
| currentIndicator.shareNumber | 주식현재가 시세 | `lstn_stcn` | 상장주수. |
| currentIndicator.eps | 국내주식 재무비율 `/uapi/domestic-stock/v1/finance/financial-ratio` | `eps` | `stac_yymm` 최신값 사용. |
| currentIndicator.per | 주식현재가 시세 | `per` | 계산 없이 직접 사용 가능. |
| currentIndicator.pbr | 주식현재가 시세 | `pbr` | 계산 없이 직접 사용 가능. |
| currentIndicator.dvr | 예탁원정보(배당일정) `/uapi/domestic-stock/v1/ksdinfo/dividend` | `per_sto_divi_amt` 합계 | 최근 1년 현금배당 합계 ÷ 현재가(`stck_prpr`). `stk_kind=보통`만 합산 권장. |
| historyData.* | 재무/손익/안정성 등 | `stac_yymm`, `eps`, `lblt_rate`, `crnt_rate` 등 | 과거 재무데이터는 일부 가능하나 PER/PBR/배당수익률 등은 근거 부족. 생략 권장. |

## 갭/제한 사항
- 상장종목 전체 목록(KOSPI/KOSDAQ) 조회 API가 이 문서에 없음. 별도 종목 리스트 소스 필요(외부 파일/다른 API).
- 실시간 현재가는 기본시세 문서의 `주식현재가 시세`에서 `stck_prpr`로 제공.
- 시가총액은 기본시세 문서의 `주식현재가 시세`에서 `hts_avls`로 제공(단위 확인 필요).
- 배당수익률(`dvr`)는 `예탁원정보(배당일정)`의 `per_sto_divi_amt` 합계와 현재가를 이용해 산출(기간 기준 정책 필요).
- `historyData`는 일부 재무 데이터만 가능하며 PER/PBR/배당수익률 등은 정확한 산출 근거가 부족함.
