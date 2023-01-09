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
