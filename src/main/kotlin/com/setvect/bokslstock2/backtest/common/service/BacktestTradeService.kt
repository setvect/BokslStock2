package com.setvect.bokslstock2.backtest.common.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.DateRange
import org.springframework.stereotype.Service

/**
 * 백테스팅 결과를 이용해 매매 분석
 *
 * 본 클래스는 매매 전략과 독립적으로 동작해야됨.
 * 즉 특정 매매전략에 의존적인 코드가 들어가면 안됨
 */
@Service
class BacktestTradeService(
    val candleRepository: CandleRepository,
) {
    /**
     * [addMonth] 주식 시작 값에서 보정할 월, 듀얼 모멘텀 처럼 기준날짜 이전의 값을 참조할 때 조건 범위를 보정할 때 사용 ㅡㅡ;
     * 날짜 보정. 백테스트 기간중 실제 시세정보가 있는 범위만 백테스트 하도록 기간을 변경
     *
     * 주의사항)
     * - 일봉 기준으로 주가여부를 판단한다. 예를 들어 분봉, 또는 주봉만 있으면 기준날짜 판단을 못함
     * - 중간에 데이터가 없는거 판단 못함. 해당 시세 시작날짜와 끝날짜를 보고 판단
     */
    fun fitBacktestRange(stockCodes: List<StockCode>, dateRange: DateRange, addMonth: Int = 0): DateRange {
        var resultRange = dateRange

        stockCodes.forEach { stockCode ->
            val range = candleRepository.findByMaxMin(stockCode.code, PeriodType.PERIOD_DAY)
            val stockRange = DateRange(range.from.plusMonths(addMonth.toLong()), range.to)
            // 주식 최초 날짜가 백테스트 날짜보다 이전이면 백테스트 날짜 사용, 아니면 주식 최초 날짜 사용
            val from = if (stockRange.from.isBefore(resultRange.from)) resultRange.from else stockRange.from
            // 주식 마지막 날짜가 백테스트 날짜보다 이후면 백테스트 날짜 사용, 아니면 주식 최초 날짜 사용
            val to = if (stockRange.to.isAfter(resultRange.to)) resultRange.to else stockRange.to
            resultRange = DateRange(from, to)
        }

        return resultRange
    }
}