package com.setvect.bokslstock2.koreainvestment.trade.controller

import com.setvect.bokslstock2.koreainvestment.trade.model.dto.AssetHistoryDto
import com.setvect.bokslstock2.koreainvestment.trade.model.dto.AssetPeriodHistoryDto
import com.setvect.bokslstock2.koreainvestment.trade.model.dto.TradeDto
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetPeriodHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import com.setvect.bokslstock2.koreainvestment.trade.repository.HistoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TradeHistoryController(
    val historyRepository: HistoryRepository
) {
    /**
     * @return 거래내역
     */
    @GetMapping("/trade/page")
    fun pageTradeList(searchForm: TradeSearchForm, pageable: Pageable): Page<TradeDto> {
        return historyRepository.pageTrade(searchForm, pageable)
    }

    /**
     * @return 종목별 자산 내역
     */
    @GetMapping("/assetHistory/page")
    fun pageAssetHistory(searchForm: AssetHistorySearchForm, pageable: Pageable): Page<AssetHistoryDto> {
        return historyRepository.pageAssetHistory(searchForm, pageable)
    }

    /**
     * @return 거래주기별 자산 합산 내역
     */
    @GetMapping("/assetPeriodHistory/page")
    fun pageAssetHistory(searchForm: AssetPeriodHistorySearchForm, pageable: Pageable): Page<AssetPeriodHistoryDto> {
        return historyRepository.pageAssetPeriodHistory(searchForm, pageable)
    }

}