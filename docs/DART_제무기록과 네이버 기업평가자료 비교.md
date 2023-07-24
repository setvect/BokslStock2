## DART_제무제표 데이터와 네이버 기업평가자료 비교

DART에서 수집하는 데이터 유형은 매우 많다. 네이버 주식 사이트에서 등에서 제공하는 기업재무 정보와 매팽이 되는 DART 필드를 찾는다.

- [네이버 주식 삼성](https://finance.naver.com/item/coinfo.naver?code=005930)를 중심으로 비교한다.

## 제무
- 연걸재무(CFS) 중심으로 계산함
  - 관련 설명자료: [IFRS](https://navercomp.wisereport.co.kr/comm/HELP_IFRS.pdf)

| 항목       | DART 조건                               | 비고                  |
| ---------- | --------------------------------------- | --------------------- |
| 자본총계   | accountNm == 자본총계 && fsDiv == CFS   | 특정 시점, 재무상태표 |
| 당기순이익 | accountNm == 당기순이익 && fsDiv == CFS | 범위 합산, 손익계산서 |
| 영업이익   | accountNm == 영업이익 && fsDiv == CFS   | 범위 합산, 손익계산서 |
| 매출액     | accountNm == 매출액 && fsDiv == CFS     | 범위 합산, 손익계산서 |
