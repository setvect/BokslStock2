package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 멀티스레드 환경해서 사용하면 안됨.
 */
@Service
class AccountService(
    private val candleRepository: CandleRepository,
    private val stockRepository: StockRepository,
) {
    /**
     * 거래 내역
     * 매매 순서를 오름차순으로 기록
     */
    private val tradeHistory = mutableListOf<TradeNeo>()
    var accountCondition: AccountCondition = AccountCondition(0.0, 0.0, 0.0)

    /**
     * @return 매매 내역 바탕으로 건별 매매 결과 목록
     */
    fun calculateTradeResult(): List<TradeResult> {
        val result = mutableListOf<TradeResult>()
        // 종목 잔고
        val averagePriceMap = mutableMapOf<StockCode, StockAccount>()
        var cash = accountCondition.cash
        // 거래 시작일과 종료일 범위를 구함
        val tradeDateRange =
            DateRange(tradeHistory.first().tradeDate.with(LocalTime.MIN), tradeHistory.last().tradeDate.with(LocalTime.MAX))
        // 거래 기간에 대한 종목별 일봉을 맵으로 구함
        val stockCandleMap = tradeHistory.map { it.stockCode }
            .distinct()
            .associateWith { stockCode ->
                val list = candleRepository.findByRange(stockCode.code, PeriodType.PERIOD_DAY, tradeDateRange.from, tradeDateRange.to)
                list.associateBy { it.candleDateTime.toLocalDate() }
            }

        tradeHistory.forEach { tradeNeo ->
            val stockAccount = averagePriceMap.getOrDefault(tradeNeo.stockCode, StockAccount(0, 0.0))
            averagePriceMap[tradeNeo.stockCode] = stockAccount
            var yieldRate = 0.0
            var feePrice = 0.0
            var gains = 0.0
            if (tradeNeo.tradeType == TradeType.BUY) {
                feePrice = tradeNeo.price * tradeNeo.qty * accountCondition.feeBuy
                stockAccount.buy(tradeNeo.price, tradeNeo.qty)
                cash -= tradeNeo.price * tradeNeo.qty + feePrice
            } else if (tradeNeo.tradeType == TradeType.SELL) {
                feePrice = tradeNeo.price * tradeNeo.qty * accountCondition.feeSell
                val averagePrice = stockAccount.getAveragePrice()
                yieldRate = (tradeNeo.price - averagePrice) / averagePrice
                stockAccount.sell(tradeNeo.qty)
                cash += tradeNeo.price * tradeNeo.qty - feePrice
                gains = (tradeNeo.price - averagePrice) * tradeNeo.qty
            }

            val purchasePrice = averagePriceMap.map { it.value.totalBuyPrice }.sum()
            // 거래 날짜 종가 기준으로 주식 평가 금액을 계산
            val stockEvalPrice = averagePriceMap.map { (stockCode, stockAccount) ->
                // 해당 종목, 날짜의 종가를 구함
                val candle = getNearCandle(stockCandleMap, stockCode, tradeNeo.tradeDate.toLocalDate())
                val price = candle.closePrice
                price * stockAccount.qty
            }.sum()

            result.add(
                TradeResult(
                    stockCode = tradeNeo.stockCode,
                    tradeType = tradeNeo.tradeType,
                    yieldRate = yieldRate,
                    price = tradeNeo.price,
                    qty = tradeNeo.qty,
                    feePrice = feePrice,
                    gains = gains,
                    tradeDate = tradeNeo.tradeDate,
                    cash = cash,
                    purchasePrice = purchasePrice,
                    stockEvalPrice = stockEvalPrice,
                    memo = tradeNeo.memo
                )
            )
        }
        return result
    }

    private fun getNearCandle(
        stockCandleMap: Map<StockCode, Map<LocalDate, CandleEntity>>,
        stockCode: StockCode,
        localDate: LocalDate
    ): CandleEntity {
        // 해당 종목, 날짜의 종가를 구함. 종목 정보가 없으면 예외 발생.
        val candleMap = stockCandleMap[stockCode] ?: throw IllegalArgumentException("종목 정보가 없음. 종목코드: ${stockCode.code}")
        // 날짜가 없으면 최대 10일 전까지 찾음. 10일 이후에도 없으면 예외 발생
        for (i in 0..10) {
            return candleMap[localDate.minusDays(i.toLong())] ?: continue
        }
        throw IllegalArgumentException("종목 정보가 없음. 종목코드: ${stockCode.code}, 날짜: $localDate")
    }

    /**
     * @return 모든 매매 종목을 반환
     */
    fun getStockCodeList(): List<StockCode> {
        return tradeHistory.map { it.stockCode }.distinct()
    }

    fun addTradeHistory(tradeNeo: TradeNeo) {
        tradeHistory.add(tradeNeo)
    }

    /**
     * @return 매수 종목이 1개 이상인 종목 반환
     */
    @Deprecated("필요 없음. github copilot 테스트용 ㅎㅎ")
    fun getStockCodeListBuy(): List<StockCode> {
        // 종목 기준으로 매수는 합, 매도는 빼기를 해 맵을 만듦
        // Key: 종목, Value: 매수-매도
        val map = tradeHistory.groupBy { it.stockCode }.mapValues { (_, list) ->
            list.sumOf { if (it.tradeType == TradeType.BUY) it.qty else -it.qty }
        }
        return map.filter { it.value > 0 }.map { it.key }
    }

    // 초기 현금, 매매 수수료 정보
    data class AccountCondition(
        /**  투자금액 */
        val cash: Double,
        /** 수 수수료 */
        val feeBuy: Double,
        /** 도 수수료 */
        val feeSell: Double,
    )

    /**
     * Trade 클래스 대체
     * 순수하게 매매에 관한 정보만 담고 있음.
     * 예를 들어 매도 수익, 수수료는 없음. 이건 계산으로 얻을 수 있기 때문에 제외했음.
     * 다 정리되면 TradeNeo -> Trade로 변경
     */
    data class TradeNeo(
        /** 매매 종목 */
        val stockCode: StockCode,
        /** 매매 종류 */
        val tradeType: TradeType,
        /**
         * 거래 단가
         * - 매수일 경우 매수 단가
         * - 매도일 경우 매도 단가
         */
        val price: Double,
        /** 매매 수량 */
        val qty: Int,
        /**  거래시간 */
        val tradeDate: LocalDateTime,
        /**
         * 거래 메모
         * 적용 조건 이름 등 저장
         */
        val memo: String = ""
    )

    // 건별 매매 결과
    data class TradeResult(
        /** 매매 종목 */
        val stockCode: StockCode,

        val tradeType: TradeType,
        /**
         * 매도시 수익률
         * 소수로 표현, 1->100%, -0.02 -> -2%
         * 매수는 0으로 표현
         */
        val yieldRate: Double,
        /**
         * 거래 단가
         * - 매수일 경우 매수 단가
         * - 매도일 경우 매도 단가
         */
        val price: Double,
        /**매수 수량 */
        val qty: Int,
        /**매매 수수료 */
        val feePrice: Double,
        /**투자 수익 금액 */
        val gains: Double,
        /**거래시간 */
        val tradeDate: LocalDateTime,
        /**해당 거래 후 현금 */
        val cash: Double,
        /** 해당 매매 완료 후 계좌 전체 주식 구입 가격 */
        val purchasePrice: Double,
        /** 해당 매매 완료 후 계좌 전체 주식 평가 금액 */
        val stockEvalPrice: Double,
        /** 거래 메모. 적용 조건 이름 등 저장 */
        val memo: String = ""
    ) {
        /** 주식 평가금 + 현금 */
        fun getEvalPrice(): Double {
            return cash + stockEvalPrice
        }
    }

    data class StockAccount(
        var qty: Int,
        var totalBuyPrice: Double,
    ) {
        /** 매수 평단가 */
        fun getAveragePrice(): Double {
            return totalBuyPrice / qty
        }

        /** 매수 */
        fun buy(price: Double, qty: Int) {
            this.qty += qty
            this.totalBuyPrice += price * qty
        }

        /** 매도 */
        fun sell(qty: Int) {
            // 매도 후 수량이 마이너스면 예외 발생
            if (this.qty - qty < 0) {
                throw IllegalArgumentException("매도 수량이 현재 수량보다 많음. 현재 수량:${this.qty}, 매도 수량:$qty")
            }
            this.totalBuyPrice -= getAveragePrice() * qty
            this.qty -= qty
        }
    }
}