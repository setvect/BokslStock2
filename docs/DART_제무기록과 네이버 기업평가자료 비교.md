## DART_제무제표 데이터와 네이버 기업평가자료 비교

## DART API 설명
- [다중회사 주요 계정](https://opendart.fss.or.kr/guide/detail.do?apiGrpCd=DS003&apiId=2019017)
- [단일회사 전체 재무제표](https://opendart.fss.or.kr/guide/detail.do?apiGrpCd=DS003&apiId=2019020)
- [배당에 관한 사항](https://opendart.fss.or.kr/guide/detail.do?apiGrpCd=DS002&apiId=2019005)
- [주식의 총수 현황](https://opendart.fss.or.kr/guide/detail.do?apiGrpCd=DS002&apiId=2020002)

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


## 특이사항
- `매출액` 정보가 없는 회사가 있음
  - 이 경우 `단일회사 전체 재무제표` 에서 `영업수익` 정보를 가져옴
  - 일반 제조업과 서비스업 손익 계산서가 다르구나 
    - 참고: https://m.blog.naver.com/donghm/201651215