# 1. 복슬스톡 테이블 설계서

## 1.1. 수집

### 1.1.1. CA_STOCK: 주식 종목

| Column Name | Attribute Name | Key | Type     | Len | Not Null | Description         |
| ----------- | -------------- | --- | -------- | --- | -------- | ------------------- |
| STOCK_SEQ   | 일련번호       | PK  | BIGINT   |     | Y        |                     |
| NAME        | 종목명         |     | VARCHAR  | 100 | Y        |                     |
| CODE        | 종목코드       |     | VARCHAR  | 20  | Y        | 005930, 233740, ... |
| REG_DATE    | 등록일         |     | DATETIME |     | Y        |                     |
| EDIT_DATE   | 마지막 수정일  |     | DATETIME |     | Y        |                     |

### 1.1.2. CB_CANDLE: 시세 정보

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

## 1.2. 백테스트

### 1.2.3. FA_RB_CONDITION: 리벨러싱 전략 조건

| Column Name   | Attribute Name   | Key | Type     | Len | Not Null | Description                           |
| ------------- | ---------------- | --- | -------- | --- | -------- | ------------------------------------- |
| CONDITION_SEQ | 일련번호         | PK  | BIGINT   |     | Y        |                                       |
| STOCK_SEQ     | 종목             | FK  | BIGINT   |     | Y        | CA_STOCK                              |
| PERIOD_TYPE   | 매매 주기        |     | VARCHAR  | 20  | Y        | PERIOD_DAY, PERIOD_WEEK, PERIOD_MONTH |
| COMMENT       | 조건에 대한 설명 |     | VARCHAR  | 100 | N        |                                       |
| REG_DATE      | 등록일           |     | DATETIME |     | Y        |                                       |
| EDIT_DATE     | 마지막 수정일    |     | DATETIME |     | Y        |                                       |

### 1.2.4. FB_RB_TRADE: 리벨러싱 전략 조건

| Column Name   | Attribute Name     | Key | Type     | Len | Not Null | Description     |
| ------------- | ------------------ | --- | -------- | --- | -------- | --------------- |
| TRADE_SEQ     | 일련번호           | PK  | BIGINT   |     | Y        |                 |
| CONDITION_SEQ | 매매 조건 일련번호 | FK  | BIGINT   |     | Y        | FA_RB_CONDITION |
| TRADE_TYPE    | 매수/매도          |     | VARCHAR  | 20  | Y        | BUY, SELL       |
| YIELD         | 매도시 수익률      |     | DOUBLE   |     | Y        |                 |
| UNIT_PRICE    | 거래 단가          |     | DOUBLE   |     | Y        |                 |
| TRADE_DATE    | 거래시간           |     | DATETIME |     | Y        |                 |

- Index
    - TRADE_DATE

### 1.2.3. GA_VBS_CONDITION: 변동성돌파 전략 조건

| Column Name        | Attribute Name       | Key | Type     | Len | Not Null | Description                           |
| ------------------ | -------------------- | --- | -------- | --- | -------- | ------------------------------------- |
| CONDITION_SEQ      | 일련번호             | PK  | BIGINT   |     | Y        |                                       |
| STOCK_SEQ          | 종목                 | FK  | BIGINT   |     | Y        | CA_STOCK                              |
| PERIOD_TYPE        | 매매 주기            |     | VARCHAR  | 20  | Y        | PERIOD_DAY, PERIOD_WEEK, PERIOD_MONTH |
| K_RATE             | 변동성 비율          |     | DOUBLE   |     | Y        |                                       |
| MA_PERIOD          | 이동평균 주기        |     | INTEGER  |     | Y        |                                       |
| UNIT_ASK_PRICE     | 호가단위             |     | DOUBLE   |     | Y        |                                       |
| GAP_RISEN_SKIP     | 갭 상승 시 매도 넘김 |     | VARCHAR  | 1   | Y        | Y, N                                  |
| ONLY_ONE_DAY_TRADE | 하루에 한번 거래     |     | VARCHAR  | 1   | Y        | Y, N                                  |
| COMMENT            | 조건에 대한 설명     |     | VARCHAR  | 100 | N        |                                       |
| REG_DATE           | 등록일               |     | DATETIME |     | Y        |                                       |
| EDIT_DATE          | 마지막 수정일        |     | DATETIME |     | Y        |                                       |

### 1.2.4. GB_VBS_TRADE: 변동성돌파 전략 조건

| Column Name   | Attribute Name        | Key | Type     | Len | Not Null | Description      |
| ------------- | --------------------- | --- | -------- | --- | -------- | ---------------- |
| TRADE_SEQ     | 일련번호              | PK  | BIGINT   |     | Y        |                  |
| CONDITION_SEQ | 매매 조건 일련번호    | FK  | BIGINT   |     | Y        | GA_VBS_CONDITION |
| TRADE_TYPE    | 매수/매도             |     | VARCHAR  | 20  | Y        | BUY, SELL        |
| MA_PRICE      | 매매 시 이동평균 가격 |     | DOUBLE   |     | Y        |                  |
| YIELD         | 매도시 수익률         |     | DOUBLE   |     | Y        |                  |
| UNIT_PRICE    | 거래 단가             |     | DOUBLE   |     | Y        |                  |
| TRADE_DATE    | 거래시간              |     | DATETIME |     | Y        |                  |

- Index
    - TRADE_DATE

### 1.2.5. HA_MABS_CONDITION: 이평선 돌파 백테스트 조건

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

### 1.2.6. HB_MABS_TRADE: 이평선 돌파 백테스트 매매 건별 정보

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
