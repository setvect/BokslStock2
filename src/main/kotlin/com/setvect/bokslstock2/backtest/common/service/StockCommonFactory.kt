package com.setvect.bokslstock2.backtest.common.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.util.StockByDateCandle
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

    fun createStockCommonFactory(accountCondition: AccountService.AccountCondition, backtestCondition: AccountService.BacktestCondition): AccountService {
        return AccountService(this, accountCondition, backtestCondition)
    }
}