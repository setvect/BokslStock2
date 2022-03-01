package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateUtil
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 듀얼모멘텀 백테스트
 */
@Service
class DmAnalysisService(
    private val stockRepository: StockRepository,
    private val movingAverageService: MovingAverageService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun runTest(dmCondition: DmAnalysisCondition) {
        val stocks = dmCondition.stockCodes.toMutableList()
        if (dmCondition.holdCode != null) {
            stocks.add(dmCondition.holdCode)
        }
        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = stocks.associateWith { code ->
            movingAverageService.getMovingAverage(
                code,
                dmCondition.periodType,
                Collections.emptyList()
            )
                .associateBy { it.candleDateTimeStart.withDayOfMonth(1) }
        }

        var current =
            DateUtil.fitMonth(dmCondition.basic.range.from.withDayOfMonth(1), dmCondition.periodType.getDeviceMonth())

        while (current.isBefore(dmCondition.basic.range.to)) {
            log.info(current.toString())
            stockPriceIndex.entries.forEach { stockEntry ->
                val candle = stockEntry.value[current]
                    ?: throw RuntimeException("${stockEntry.key}에 대한 ${current.toString()} 시세가 없습니다.")

                val stock = stockRepository.findByCode(stockEntry.key).get()

                log.info(
                    "\t${stock.name}(${stock.code}): ${candle.candleDateTimeStart}~${candle.candleDateTimeEnd} - " +
                            "O: ${candle.openPrice}, H: ${candle.highPrice}, L: ${candle.lowPrice}, C:${candle.closePrice}, ${candle.periodType}"
                )

            }
            current = current.plusMonths(dmCondition.periodType.getDeviceMonth().toLong())
        }
    }
}