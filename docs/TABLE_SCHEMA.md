# 1. 복슬스톡 테이블 설계서

## 1.1. 수집

### 1.1.1. CA_STOCK: 주식 종목

| Column Name | Attribute Name | Key | Type     | Len | Not Null | Description         |
| ----------- | -------------- | --- | -------- | --- | -------- | ------------------- |
| STOCK_SEQ   | 일련번호       | PK  | INTEGER  |     | Y        |                     |
| NAME        | 종목명         |     | VARCHAR  | 100 | Y        |                     |
| CODE        | 종목코드       |     | VARCHAR  | 20  | Y        | 005930, 233740, ... |
| REG_DATE    | 등록일         |     | DATETIME |     | Y        |                     |
| EDIT_DATE   | 마지막 수정일  |     | DATETIME |     | Y        |                     |

### 1.1.2. CB_CANDLE: 시세 정보

| Column Name      | Attribute Name | Key | Type     | Len | Not Null | Description                                             |
| ---------------- | -------------- | --- | -------- | --- | -------- | ------------------------------------------------------- |
| CANDLE_SEQ       | 일련번호       | PK  | INTEGER  |     | Y        |                                                         |
| STOCK_SEQ        | 종목           | FK  | INTEGER  |     | Y        |                                                         |
| CANDLE_DATE_TIME | 시세 기준 날짜 | IDX | DATETIME |     | Y        |                                                         |
| PERIOD_TYPE      | 기준 기간      | IDX | VARCHAR  | 20  | Y        | PERIOD_DAY: 일봉, PERIOD_WEEK: 주봉, PERIOD_MONTH: 월봉 |
| OPEN_PRICE       | 시가           |     | INTEGER  |     | Y        |                                                         |
| HIGH_PRICE       | 고가           |     | INTEGER  |     | Y        |                                                         |
| LOW_PRICE        | 저가           |     | INTEGER  |     | Y        |                                                         |
| CLOSE_PRICE      | 종가           |     | INTEGER  |     | Y        |                                                         |

## 1.2. 백테스트

### 1.2.1. XA_MABS_CONDITION: 이평선 돌파 백테스트 조건

| Column Name        | Attribute Name     | Key | Type     | Len | Not Null | Description                           |
| ------------------ | ------------------ | --- | -------- | --- | -------- | ------------------------------------- |
| MABS_CONDITION_SEQ | 일련번호           | PK  | INTEGER  |     | Y        |                                       |
| MARKET             | 코인 이름          |     | VARCHAR  | 20  | Y        | KRW-BTC, KRW-ETH,...                  |
| PERIOD_TYPE        | 매매 주기          |     | VARCHAR  | 20  | Y        | PERIOD_DAY, PERIOD_WEEK, PERIOD_MONTH |
| UP_BUY_RATE        | 상승 매수률        |     | NUMBER   |     | Y        |                                       |
| DOWN_BUY_RATE      | 하락 매도률        |     | NUMBER   |     | Y        |                                       |
| SHORT_PERIOD       | 단기 이동평균 기간 |     | INTEGER  |     | Y        |                                       |
| LONG_PERIOD        | 장기 이동평균 기간 |     | INTEGER  |     | Y        |                                       |
| COMMENT            | 조건에 대한 설명   |     | VARCHAR  | 20  | N        |                                       |
| REG_DATE           | 등록일             |     | DATETIME |     | Y        |                                       |
| EDIT_DATE          | 마지막 수정일      |     | DATETIME |     | Y        |                                       |

### 1.2.2. XB_MABS_TRADE: 이평선 돌파 백테스트 매매 건별 정보

| Column Name        | Attribute Name     | Key | Type     | Len | Not Null | Description       |
| ------------------ | ------------------ | --- | -------- | --- | -------- | ----------------- |
| TRADE_SEQ          | 일련번호           | PK  | INTEGER  |     | Y        |                   |
| MABS_CONDITION_SEQ | 매매 조건 일련번호 | FK  | INTEGER  |     | Y        | XA_MABS_CONDITION |
| TRADE_TYPE         | 매수/매도          |     | VARCHAR  | 20  | Y        | BUY, SELL         |
| HIGH_YIELD         | 최고 수익률        |     | NUMBER   |     | Y        |                   |
| LOW_YIELD          | 최저 수익률        |     | NUMBER   |     | Y        |                   |
| MA_SHORT           | 단기 이동평균 가격 |     | NUMBER   |     | Y        |                   |
| MA_LONG            | 장기 이동평균 가격 |     | NUMBER   |     | Y        |                   |
| YIELD              | 매도시 수익률      |     | NUMBER   |     | Y        |                   |
| UNIT_PRICE         | 거래 단가          |     | NUMBER   |     | Y        |                   |
| TRADE_DATE         | 거래시간           |     | DATETIME |     | Y        |                   |

- Index
  - TRADE_DATE
