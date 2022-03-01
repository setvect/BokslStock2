package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_HALF
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_MONTH
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_QUARTER
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_WEEK
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_YEAR
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateUtil
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MovingAverageService(
    private val stockRepository: StockRepository,
) {
    /**
     * [code] 종목에 대한 [group]단위로 [avgCountList]만큼 이동 평균 계산
     * @return 날짜:해당 날의 이동평균
     */
    @Transactional
    fun getMovingAverage(
        code: String, group: PeriodType, avgCountList: List<Int>
    ): List<CandleDto> {
        val stockOptional = stockRepository.findByCode(code)
        val stock = stockOptional.orElseThrow { RuntimeException("$code 종목 정보가 없습니다.") }

        val candleList = stock.candleList
        val candleGroupMap = candleList.groupByTo(TreeMap()) {
            // 날짜 기준으로 시세 그룹핑
            val groupDateTime = when (group) {
                PERIOD_DAY -> {
                    it.candleDateTime
                }
                PERIOD_WEEK -> {
                    it.candleDateTime.minusDays(it.candleDateTime.dayOfWeek.value.toLong() - 1)
                }
                PERIOD_MONTH, PERIOD_QUARTER, PERIOD_HALF, PERIOD_YEAR -> {
                    DateUtil.fitMonth(it.candleDateTime.withDayOfMonth(1), group.getDeviceMonth())
                }
            }
            groupDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0)
        }

        val groupingCandleList = candleGroupMap.entries.map {
            CandleDto(
                candleDateTimeStart = it.value.first().candleDateTime,
                candleDateTimeEnd = it.value.last().candleDateTime,
                lowPrice = it.value.minOf { p -> p.lowPrice },
                highPrice = it.value.maxOf { p -> p.highPrice },
                openPrice = it.value.first().openPrice,
                closePrice = it.value.last().closePrice,
                periodType = group
            )
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