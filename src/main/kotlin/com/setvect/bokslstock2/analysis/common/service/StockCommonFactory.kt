package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.util.StockByDateCandle
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.DateRange
import org.springframework.stereotype.Service

@Service
class StockCommonFactory(
    private val candleRepository: CandleRepository,
) {
    fun createStockByDateCandle(stockCodes: Set<StockCode>, dateRange: DateRange): StockByDateCandle {
        return StockByDateCandle(candleRepository, stockCodes, dateRange)
    }

    fun createStockCommonFactory(accountCondition: AccountService.AccountCondition): AccountService {
        return AccountService(this, accountCondition)
    }
}