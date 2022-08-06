-- 각 종목별 시세 데이터 범위
select CA.STOCK_SEQ, CA.CODE, CA.NAME, count(*), MIN(CC.CANDLE_DATE_TIME), MAX(CC.CANDLE_DATE_TIME)
from CA_STOCK CA
         join CB_CANDLE CC on CA.STOCK_SEQ = CC.STOCK_SEQ
group by CA.STOCK_SEQ
;
