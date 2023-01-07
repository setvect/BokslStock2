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
        and FORMATDATETIME(CB.CANDLE_DATE_TIME, 'HH:mm') = '09:05') AA
group by CODE;
