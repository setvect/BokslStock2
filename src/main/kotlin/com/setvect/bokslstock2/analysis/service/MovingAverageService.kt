package com.setvect.bokslstock2.analysis.service

import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_MONTH
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_WEEK
import com.setvect.bokslstock2.index.repository.StockRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.TreeMap
import kotlin.math.roundToInt

@Service
class MovingAverageService(
    private val stockRepository: StockRepository,
) {
    /**
     * [code] 종목에 대한 [group]단위로 [avgCountList]만큼 이동 평균 계산
     * @return 날짜:해당 날의 이동평균
     */
    fun getMovingAverage(
        code: String,
        group: PeriodType,
        avgCountList: List<Int>
    ): LinkedHashMap<LocalDateTime, Double> {
        val stockOptional = stockRepository.findByCode(code)
        val stock = stockOptional.orElseThrow { RuntimeException("$code 종목 정보가 없습니다.") }

        val candleList = stock.candleList
        val candleGroupMap = candleList.groupByTo(TreeMap()) {
            val localDateTime = when (group) {
                PERIOD_DAY -> {
                    it.candleDateTime

                }
                PERIOD_WEEK -> {
                    it.candleDateTime
                        .minusDays(it.candleDateTime.dayOfWeek.value.toLong() - 1)
                }
                PERIOD_MONTH -> {
                    it.candleDateTime
                        .withDayOfMonth(1)
                }
            }
            localDateTime.withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        }

        val groupingCandleList = candleGroupMap.entries.map {
            CandleDto(
                candleDateTime = it.value.last().candleDateTime,
                lowPrice = it.value.minOf { p -> p.lowPrice },
                highPrice = it.value.maxOf { p -> p.highPrice },
                openPrice = it.value.first().openPrice,
                closePrice = it.value.last().closePrice,
                periodType = group
            )
        }

        groupingCandleList.forEach {
            println("${it.candleDateTime} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}")
        }

        avgCountList.forEach { avgCount ->
            for (idx in avgCount - 1 until groupingCandleList.size) {
                val average = groupingCandleList.stream()
                    .skip((idx - avgCount + 1).toLong())
                    .limit(avgCount.toLong())
                    .mapToInt(CandleDto::closePrice)
                    .average()

                groupingCandleList[idx].average[avgCount] = average.asDouble.roundToInt()
            }
        }

        groupingCandleList.forEach {
            val avgInfo =
                avgCountList.map { avgCount -> "이동평균(${avgCount}): ${it.average[avgCount]}" }.toList()
                    .joinToString(", ")
            println("${it.candleDateTime} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}, ${it.periodType}, $avgInfo")
        }
        return LinkedHashMap()
    }
}