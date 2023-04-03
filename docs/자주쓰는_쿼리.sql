-- 각 종목별 시세 데이터 범위
select CA.STOCK_SEQ, CA.CODE, CA.NAME, count(*), MIN(CC.CANDLE_DATE_TIME), MAX(CC.CANDLE_DATE_TIME)
from CA_STOCK CA
         join CB_CANDLE CC on CA.STOCK_SEQ = CC.STOCK_SEQ
group by CA.STOCK_SEQ
;

-- 개장 최소 5분봉 시초가와 종가 비교

select CA.CODE,
       CA.NAME,
       CB.PERIOD_TYPE,
       CB.CANDLE_DATE_TIME,
       FORMATDATETIME(CB.CANDLE_DATE_TIME, 'HH:mm') hh_mm,
       CB.OPEN_PRICE,
       CB.CLOSE_PRICE,
       CB.CLOSE_PRICE - CB.OPEN_PRICE               DIFF
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where CA.CODE = '091170'
  and PERIOD_TYPE = 'PERIOD_MINUTE_5'
  and FORMATDATETIME(CB.CANDLE_DATE_TIME, 'HH:mm') = '09:05';

select CODE,
       NAME,
       count(*),
       sum(DIFF),
       avg(DIFF),
       sum(CASEWHEN(DIFF > 0, 1, 0)) `증가`,
       sum(CASEWHEN(DIFF < 0, 1, 0)) `감소`,
       sum(CASEWHEN(DIFF = 0, 1, 0)) `같음`
from (select CA.CODE,
             CA.NAME,
             CB.PERIOD_TYPE,
             CB.CANDLE_DATE_TIME,
             FORMATDATETIME(CB.CANDLE_DATE_TIME, 'HH:mm') hh_mm,
             CB.OPEN_PRICE,
             CB.CLOSE_PRICE,
             CB.CLOSE_PRICE - CB.OPEN_PRICE               DIFF
      from CA_STOCK CA
               join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
      where 1 = 1
--         and CA.CODE = '091170'
        and PERIOD_TYPE = 'PERIOD_MINUTE_5'
        and CB.CANDLE_DATE_TIME > '2022-01-01'
        and FORMATDATETIME(CB.CANDLE_DATE_TIME, 'HH:mm') = '09:05') AA
group by CODE;


-- 종목별 하나하나 데이터 조회
select *
from CA_STOCK CA
         join CB_CANDLE CC on CA.STOCK_SEQ = CC.STOCK_SEQ
where CA.CODE = '091170';

select CB.*
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where 1 = 1
  AND CA.CODE = '091170'
--   AND CB.PERIOD_TYPE = 'PERIOD_DAY'
  AND CB.PERIOD_TYPE = 'PERIOD_MINUTE_5'
  AND CB.CANDLE_DATE_TIME between '2021-08-31' and '2021-09-01'
order by CANDLE_DATE_TIME
;


select CA.NAME,
       CB.CANDLE_DATE_TIME,
       CB.OPEN_PRICE,
       CB.CLOSE_PRICE,
       CB.CLOSE_PRICE - CB.OPEN_PRICE         등락,
       (CB.CLOSE_PRICE / CB.OPEN_PRICE - 1.0) 상승률
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where code = 'CASH_5'
order by CB.CANDLE_DATE_TIME desc;


select avg(CB.CLOSE_PRICE - CB.OPEN_PRICE) 등락, avg(CB.CLOSE_PRICE / CB.OPEN_PRICE - 1.0) 상승률
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where code = '192090';

select min(CA.NAME),
       count(*)                                                               as 매매회숫,
       round(exp(avg(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE + 1.0))) - 1, 5)       as 기하평균,
       round(avg(CB.CLOSE_PRICE / CB.OPEN_PRICE), 5)                          as 산술평균,
       round(exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE))) - 1, 5)             as 누적수익률,
       round((exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE - 0.00015))) - 1), 5) as `누적수익률(매매비용 0.015%적용)`
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where 1 = 1
  and code = '192090'
  and CB.CANDLE_DATE_TIME between '2023-01-19' and '2023-01-27';

-- 갭 상승 하락 적용이 되지 않아 신뢰 할 수 없음
select CA.code,
       min(CA.NAME),
       min(CB.CANDLE_DATE_TIME)                                                                  as 시작일,
       max(CB.CANDLE_DATE_TIME)                                                                  as 종료일,
       count(*)                                                                                  as 매매회숫,
       round(exp(avg(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE + 1.0))) - 1, 5)                          as 기하평균,
       round(avg(CB.CLOSE_PRICE / CB.OPEN_PRICE), 5)                                             as 산술평균,
       round(exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE))) - 1, 5)                                as 누적수익률,
       round((exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE - 0.00015))) - 1),
             5)                                                                                  as `누적수익률(매매비용 0.015%적용)`,
       round((exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE - 0.00015))) - 1), 5) / count(*)         as `매매당 기대수익률`,
       (round((exp(sum(ln(CB.CLOSE_PRICE / CB.OPEN_PRICE - 0.00015))) - 1), 5) / count(*)) * 260 as `약식 CAGR`
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where 1 = 1
  and CB.CLOSE_PRICE <> 0
  and CB.OPEN_PRICE <> 0
  and CB.PERIOD_TYPE = 'PERIOD_DAY'
  and CB.CANDLE_DATE_TIME between '2018-01-01' and '2023-01-01'
group by CA.CODE
order by `매매당 기대수익률` desc;


select CB.*
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where CA.CODE = 'WON-DOLLAR'
order by CANDLE_DATE_TIME;

-- 변동성 돌파전략 백테스트 5분봉 데이터 조회
select CA.CODE, CB.PERIOD_TYPE, count(*), max(CB.CANDLE_DATE_TIME)
from CA_STOCK CA
         join CB_CANDLE CB on CA.STOCK_SEQ = CB.STOCK_SEQ
where CA.CODE IN('091170', '233740')
group by CA.CODE, CB.PERIOD_TYPE


select *
from GB_VBS_TRADE;
