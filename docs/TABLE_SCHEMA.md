# 1. BokslStock2 테이블 설계서

## 1.1. 매매

### 1.1.1. BA_TRADE: 거래내역

| Column Name | Attribute Name | Key | Type     | Len  | Not Null | Description                                                      |
| ----------- | -------------- | --- | -------- | ---- | -------- | ---------------------------------------------------------------- |
| TRADE_SEQ   | 일련번호       | PK  | BIGINT   |      | Y        |                                                                  |
| ACCOUNT     | 거래계좌       |     | VARCHAR  | 50   | Y        | MD5로 변환해서 저장                                              |
| CODE        | 종목코드       |     | VARCHAR  | 20   | Y        | 005930, 069500, ...                                              |
| TRADE_TYPE  | 매수/매도      |     | VARCHAR  | 20   | Y        | BUY, SELL                                                        |
| QTY         | 수량           |     | INTEGER  |      | Y        |                                                                  |
| UNIT_PRICE  | 단가           |     | NUMBER   |      | Y        |                                                                  |
| YIELD       | 수익률         |     | NUMBER   |      | Y        | 매도시 수익률, 매수일 경우 0, 소수로 표현, 1->100%, -0.02 -> -2% |
| MEMO        | 메모           |     | VARCHAR  | 1000 | N        |                                                                  |
| REG_DATE    | 거래 시간      |     | DATETIME |      | Y        |                                                                  |

### 1.1.2. BB_ASSET_HISTORY:  자산 기록

| Column Name       | Attribute Name              | Key | Type     | Len  | Not Null | Description                                             |
| ----------------- | --------------------------- | --- | -------- | ---- | -------- | ------------------------------------------------------- |
| ASSET_HISTORY_SEQ | 일련번호                    | PK  | BIGINT   |      | Y        |                                                         |
| ACCOUNT           | 거래계좌                    |     | VARCHAR  | 20   | Y        | MD5로 변환해서 저장                                     |
| ASSET_CODE        | 자산 종류(예수금, 종목코드) |     | VARCHAR  | 20   | Y        | DEPOSIT, 005930, 069500, ...                            |
| INVESTMENT        | 투자금                      |     | NUMBER   |      | Y        |                                                         |
| YIELD             | 수익률                      |     | NUMBER   |      | Y        | 소수로 표현, 1->100%, -0.02 -> -2%                      |
| MEMO              | 메모                        |     | VARCHAR  | 1000 | N        |                                                         |
| REG_DATE          | 자산 조회 시간              |     | DATETIME |      | Y        | 한 계좌의 여러 자산을 조회 할 경우 동일한 시간을 보장함 |

## 1.2. 수집

### 1.2.1. CA_STOCK: 주식 종목

| Column Name | Attribute Name | Key | Type     | Len | Not Null | Description         |
| ----------- | -------------- | --- | -------- | --- | -------- | ------------------- |
| STOCK_SEQ   | 일련번호       | PK  | BIGINT   |     | Y        |                     |
| NAME        | 종목명         |     | VARCHAR  | 100 | Y        |                     |
| CODE        | 종목코드       |     | VARCHAR  | 20  | Y        | 005930, 233740, ... |
| REG_DATE    | 등록일         |     | DATETIME |     | Y        |                     |
| EDIT_DATE   | 마지막 수정일  |     | DATETIME |     | Y        |                     |

### 1.2.2. CB_CANDLE: 시세 정보

| Column Name      | Attribute Name | Key | Type     | Len | Not Null | Description                                             |
| ---------------- | -------------- | --- | -------- | --- | -------- | ------------------------------------------------------- |
| CANDLE_SEQ       | 일련번호       | PK  | BIGINT   |     | Y        |                                                         |
| STOCK_SEQ        | 종목           | FK  | BIGINT   |     | Y        |                                                         |
| CANDLE_DATE_TIME | 시세 기준 날짜 | IDX | DATETIME |     | Y        |                                                         |
| PERIOD_TYPE      | 기준 기간      | IDX | VARCHAR  | 20  | Y        | PERIOD_DAY: 일봉, PERIOD_WEEK: 주봉, PERIOD_MONTH: 월봉 |
| OPEN_PRICE       | 시가           |     | DOUBLE   |     | Y        |                                                         |
| HIGH_PRICE       | 고가           |     | DOUBLE   |     | Y        |                                                         |
| LOW_PRICE        | 저가           |     | DOUBLE   |     | Y        |                                                         |
| CLOSE_PRICE      | 종가           |     | DOUBLE   |     | Y        |                                                         |


### 1.2.3. CC_CORPORATE_DISCLOSURE_INFO: 기업공시정보

| Column Name                   | Attribute Name     | Key | Type     | Len | Not Null | Description                      |
| ----------------------------- | ------------------ | --- | -------- | --- | -------- | -------------------------------- |
| CORPORATE_DISCLOSURE_INFO_SEQ | 일련번호           | PK  | BIGINT   |     | Y        |                                  |
| CODE                          | 종목코드           |     | VARCHAR  | 20  | Y        | 005930, 233740, ...              |
| FINANCIAL_METRIC_TYPE         | 재무제표 항목 유형 |     | VARCHAR  |     | Y        | SALES_REVENUE, TOTAL_ASSETS, ... |
| YEAR                          | 년도               |     | INTEGER  |     | Y        |                                  |
| ACCOUNT_CLOSE                 | 회계마감 기준      |     | VARCHAR  | 20  | Y        | Q1, Q2, Q3, Q4                   |
| ITEM_NAME                     | 재무제표 항목명    |     | VARCHAR  | 50  | Y        | 매출액, 영업이익                 |
| Q1_VALUE                      | 1분기값            |     | BIGINT   |     | Y        |                                  |
| Q2_VALUE                      | 2분기값            |     | BIGINT   |     | Y        |                                  |
| Q3_VALUE                      | 3분기값            |     | BIGINT   |     | Y        |                                  |
| Q4_VALUE                      | 4분기값            |     | BIGINT   |     | Y        |                                  |
| REG_DATE                      | 등록일             |     | DATETIME |     | Y        |                                  |
| EDIT_DATE                     | 마지막 수정일      |     | DATETIME |     | Y        |                                  |


## 1.3. 백테스트

### 1.3.1. HA_MABS_CONDITION: 이평선 돌파 백테스트 조건

| Column Name   | Attribute Name     | Key | Type     | Len | Not Null | Description                           |
| ------------- | ------------------ | --- | -------- | --- | -------- | ------------------------------------- |
| CONDITION_SEQ | 일련번호           | PK  | BIGINT   |     | Y        |                                       |
| STOCK_SEQ     | 종목               | FK  | BIGINT   |     | Y        | CA_STOCK                              |
| PERIOD_TYPE   | 매매 주기          |     | VARCHAR  | 20  | Y        | PERIOD_DAY, PERIOD_WEEK, PERIOD_MONTH |
| UP_BUY_RATE   | 상승 매수률        |     | DOUBLE   |     | Y        |                                       |
| DOWN_BUY_RATE | 하락 매도률        |     | DOUBLE   |     | Y        |                                       |
| SHORT_PERIOD  | 단기 이동평균 기간 |     | INTEGER  |     | Y        |                                       |
| LONG_PERIOD   | 장기 이동평균 기간 |     | INTEGER  |     | Y        |                                       |
| COMMENT       | 조건에 대한 설명   |     | VARCHAR  | 100 | N        |                                       |
| REG_DATE      | 등록일             |     | DATETIME |     | Y        |                                       |
| EDIT_DATE     | 마지막 수정일      |     | DATETIME |     | Y        |                                       |

### 1.3.2. HB_MABS_TRADE: 이평선 돌파 백테스트 매매 건별 정보

| Column Name   | Attribute Name     | Key | Type     | Len | Not Null | Description       |
| ------------- | ------------------ | --- | -------- | --- | -------- | ----------------- |
| TRADE_SEQ     | 일련번호           | PK  | BIGINT   |     | Y        |                   |
| CONDITION_SEQ | 매매 조건 일련번호 | FK  | BIGINT   |     | Y        | HA_MABS_CONDITION |
| TRADE_TYPE    | 매수/매도          |     | VARCHAR  | 20  | Y        | BUY, SELL         |
| HIGH_YIELD    | 최고 수익률        |     | DOUBLE   |     | Y        |                   |
| LOW_YIELD     | 최저 수익률        |     | DOUBLE   |     | Y        |                   |
| MA_SHORT      | 단기 이동평균 가격 |     | DOUBLE   |     | Y        |                   |
| MA_LONG       | 장기 이동평균 가격 |     | DOUBLE   |     | Y        |                   |
| YIELD         | 매도시 수익률      |     | DOUBLE   |     | Y        |                   |
| UNIT_PRICE    | 거래 단가          |     | DOUBLE   |     | Y        |                   |
| TRADE_DATE    | 거래시간           |     | DATETIME |     | Y        |                   |

- Index
  - TRADE_DATE