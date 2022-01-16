
# 1. 복슬스톡 테이블 설계서
## 1.1. 백테스트
## 1.2. 수집

### 1.2.1. CA_STOCK: 주식 종목
| Column Name | Attribute Name | Key | Type     | Len | Not Null | Description         |
| ----------- | -------------- | --- | -------- | --- | -------- | ------------------- |
| STOCK_ID    | 일련번호       | PK  | INTEGER  |     | Y        |                     |
| NAME        | 종목명         |     | VARCHAR  | 100 | Y        |                     |
| CODE        | 종목코드       |     | VARCHAR  | 20  | Y        | 005930, 233740, ... |
| REG_DATE    | 등록일         |     | DATETIME |     | Y        |                     |
| EDIT_DATE   | 마지막 수정일  |     | DATETIME |     | Y        |                     |

### 1.2.2. CB_CANDLE: 시세 정보
| Column Name      | Attribute Name | Key | Type     | Len | Not Null | Description                                             |
| ---------------- | -------------- | --- | -------- | --- | -------- | ------------------------------------------------------- |
| CANDLE_SEQ       | 일련번호       | PK  | INTEGER  |     | Y        |                                                         |
| STOCK_ID         | 종목           | FK  | INTEGER  |     | Y        |                                                         |
| CANDLE_DATE_TIME | 시세 기준 날짜 | IDX | DATETIME |     | Y        |                                                         |
| PERIOD_TYPE      | 기준 기간      | IDX | VARCHAR  | 20  | Y        | PERIOD_DAY: 일봉, PERIOD_WEEK: 주봉, PERIOD_MONTH: 주봉 |
| OPENING_PRICE    | 시가           |     | INTEGER  |     | Y        |                                                         |
| HIGH_PRICE       | 고가           |     | INTEGER  |     | Y        |                                                         |
| LOW_PRICE        | 저가           |     | INTEGER  |     | Y        |                                                         |
| TRADE_PRICE      | 종가           |     | INTEGER  |     | Y        |                                                         |

