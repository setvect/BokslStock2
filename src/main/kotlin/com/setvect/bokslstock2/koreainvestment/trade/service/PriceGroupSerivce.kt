package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.koreainvestment.trade.model.response.MinutePriceResponse
import com.setvect.bokslstock2.util.ApplicationUtil
import org.springframework.stereotype.Service
import java.util.*

@Service
class PriceGroupService {
    companion object {
        fun groupByMinute5(
            minutePrice: MinutePriceResponse,
            stockCode: StockCode
        ): MutableList<CandleDto> {
            // 과거 데이터를 먼저 처리 하기 위해 reversed() 적용
            val candleGroupMap = minutePrice.output2.reversed().groupByTo(TreeMap()) {
                return@groupByTo ApplicationUtil.fitStartDateTime(PeriodType.PERIOD_MINUTE_5, it.baseTime())
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
                    candleDateTimeStart = candleGroup.second.first().baseTime(),
                    candleDateTimeEnd = candleGroup.second.last().baseTime(),
                    beforeCandleDateTimeEnd = beforeCandle.second.last().baseTime(),
                    beforeClosePrice = beforeCandle.second.last().stckPrpr.toDouble(),
                    openPrice = candleGroup.second.first().stckOprc.toDouble(),
                    highPrice = candleGroup.second.maxOf { p -> p.stckHgpr.toDouble() },
                    lowPrice = candleGroup.second.minOf { p -> p.stckLwpr.toDouble() },
                    closePrice = candleGroup.second.last().stckPrpr.toDouble(),
                    periodType = PeriodType.PERIOD_MINUTE_5
                )
                groupingCandleList.add(candle)
            }
            return groupingCandleList
        }
    }
}