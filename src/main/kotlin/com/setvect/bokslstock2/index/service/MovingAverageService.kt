package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MovingAverageService(
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * [stockCode] 종목에 대한 [selectPeriod] 시계을 조회해 [groupPeriod]단위로 [avgCountList]만큼 이동 평균 계산
     * @return 날짜:해당 날의 이동평균
     */
    @Transactional
    fun getMovingAverage(
        stockCode: StockCode,
        selectPeriod: PeriodType,
        groupPeriod: PeriodType,
        avgCountList: List<Int> = listOf(),
        dateRange: DateRange = DateRange.maxRange
    ): List<CandleDto> {
        val stockOptional = stockRepository.findByCode(stockCode.code)
        val stock = stockOptional.orElseThrow { RuntimeException("$stockCode 종목 정보가 없습니다.") }

        val candleList = candleRepository.findByRange(stock.code, selectPeriod, dateRange.from, dateRange.to)
        val candleGroupMap = candleList
            .groupByTo(TreeMap()) {
                return@groupByTo ApplicationUtil.fitStartDateTime(groupPeriod, it.candleDateTime)
            }

        val candleGroupList = candleGroupMap.entries.map { Pair(it.key, it.value) }

        val groupingCandleList = mutableListOf<CandleDto>()
        for (i in candleGroupList.indices) {
            val beforeCandle = if (i == 0) {
                candleGroupList[i]
            } else {
                candleGroupList[i - 1]
            }
            val candleGroup = candleGroupList[i]
            val candle = CandleDto(
                stockCode = stockCode,
                candleDateTimeStart = candleGroup.second.first().candleDateTime,
                candleDateTimeEnd = candleGroup.second.last().candleDateTime,
                beforeCandleDateTimeEnd = beforeCandle.second.last().candleDateTime,
                beforeClosePrice = beforeCandle.second.last().closePrice,
                openPrice = candleGroup.second.first().openPrice,
                highPrice = candleGroup.second.maxOf { p -> p.highPrice },
                lowPrice = candleGroup.second.minOf { p -> p.lowPrice },
                closePrice = candleGroup.second.last().closePrice,
                periodType = groupPeriod
            )
            groupingCandleList.add(candle)
        }

        // 이동평균에는 현재 기간을 포함하지 않음
        avgCountList.forEach { avgCount ->
            for (idx in avgCount - 1 until groupingCandleList.size) {
                if (idx - avgCount < 0) {
                    continue
                }
                val average = groupingCandleList.stream()
                    .skip((idx - avgCount).toLong())
                    .limit(avgCount.toLong())
                    .mapToDouble(CandleDto::closePrice).average()

                if (average.isEmpty) {
                    continue
                }

                groupingCandleList[idx].average[avgCount] = average.asDouble
            }
        }

        return groupingCandleList
    }

}